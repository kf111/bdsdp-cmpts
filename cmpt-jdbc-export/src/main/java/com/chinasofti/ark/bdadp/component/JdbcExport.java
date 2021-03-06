package com.chinasofti.ark.bdadp.component;

import com.chinasofti.ark.bdadp.component.api.Configureable;
import com.chinasofti.ark.bdadp.component.api.RunnableComponent;
import com.chinasofti.ark.bdadp.util.io.FileUtil;

import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wumin on 2016/9/23.
 *
 * Fixed by white on 2017/7/7.
 */
public class JdbcExport extends RunnableComponent implements Configureable {

  private final static String DEST_CHARSET = "UTF-8";
  private String driver;
  private String url;
  private String user;
  private String pwd;
  private String sql;
  private Long fileRecordNum;
  private String destPath;
  private String destName;
  private String separator;
  private String dbCharset;

  private int retryCount;
  private int retryPeriod;
  private int failedTimes;
  private boolean success;

  private ResultSet rs = null;
  private Statement stmt = null;
  private Connection conn = null;

  public JdbcExport(String id, String name, Logger log) {
    super(id, name, log);
  }

  @Override
  public void configure(ComponentProps props) {
    driver = props.getString("jdbc_driver");
    url = props.getString("jdbc_url");
    user = props.getString("jdbc_user");
    pwd = props.getString("jdbc_pwd");
    sql = props.getString("jdbc_sql");
    String s = props.getString("file_record_num", "-1");
    fileRecordNum = Long.valueOf(s);
    destPath = props.getString("dest_path");
    destName = props.getString("dest_name");
    separator = props.getString("separator", ",");
    dbCharset = props.getString("charset", "UTF-8");

    retryCount = props.getInt("retryCount", 4);
    retryPeriod = props.getInt("retryPeriod", 60);

    checkParams();

    File tempDir = new File(destPath);
    if (!tempDir.exists()) {
      tempDir.mkdirs();
    }
    debug("Output folder automatically created successfully!");
  }

  @Override
  public void run() {
    try {
      conn = getConnection(driver, url, user, pwd);
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      if (fileRecordNum == -1) {
        writeToFile(rs, new File(FileUtil.toPath(destPath) + destName));
      } else {
        writeToFiles(rs, new File(FileUtil.toPath(destPath) + destName), fileRecordNum);
      }

      success = true;
    } catch (Exception e) {
      failedTimes += 1;
      error(String.format("Task %s failed %s times, most recent failure: %s", getId(), failedTimes,
                          e.getMessage()));

    } finally {
      close();
    }

    if (!success) {
      if (failedTimes > retryCount) {
        throw new RuntimeException(
            "Task aborted due to the number of attempts to retry has exceeded.");
      } else {
        info(String.format("Try again after %s seconds...", retryPeriod));
        try {
          Thread.sleep(retryPeriod * 1000);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }

        run();
      }

    }

  }

  private void close() {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    if (stmt != null) {
      try {
        stmt.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  private void checkParams() {
    if (driver == null || "".equals(driver.trim())) {
      throw new RuntimeException("Jdbc driver is required.");
    }
    if (url == null || "".equals(url.trim())) {
      throw new RuntimeException("Jdbc url is required.");
    }
    if (sql == null || "".equals(sql.trim())) {
      throw new RuntimeException("Jdbc sql is required.");
    }
    if (destPath == null || "".equals(destPath.trim())) {
      throw new RuntimeException("Dest path is required.");
    }
    if (destName == null || "".equals(destName.trim())) {
      throw new RuntimeException("Dest name is required.");
    }
  }

  private void writeToFiles(ResultSet rs, File to, Long fileRecordNum) {
    int cnt = 1;
    to.getParentFile().mkdirs();
    String fileName = to.getAbsolutePath();
    String preFileName = FileUtil.getFileNameNoEx(fileName); // 获取不带扩展名的文件名
    String suffix = FileUtil.getExtensionName(fileName); // 文件扩展名
    try {
      while (!rs.isAfterLast()) {
        writeToFile(rs, new File(preFileName + "_" + cnt + "." + suffix), fileRecordNum);
        cnt++;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Write to files failed, details are: " + e.getMessage());
    }

  }

  private Long writeToFile(ResultSet rs, File to) {
    return writeToFile(rs, to, Long.valueOf(-1));
  }

  @SuppressWarnings("finally")
  private Long writeToFile(ResultSet rs, File to, Long fileRecordNum) {

    if (to.exists() && to.delete()) {
      info("delete file: " + to);
    }

    if (!to.exists()) {
      // to.mkdirs();
      BufferedWriter bw = null;
      try {
        to.createNewFile();
        Long rowCnt = 0L;
        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(to), DEST_CHARSET));

        // Field
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
          sb.append(rs.getMetaData().getColumnName(i));
          sb.append(separator);
        }
        sb.deleteCharAt(sb.length() - 1);
        bw.write(sb.toString());
        bw.newLine();

        // Data
        while (rs.next()) {
          sb = new StringBuilder();
          for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            sb.append(rs.getString(i).trim());
            sb.append(separator);
          }
          sb.deleteCharAt(sb.length() - 1);
          bw.write(sb.toString());
          bw.newLine();
          rowCnt++;
          if (fileRecordNum.equals(rowCnt)) // end write to file
          {
            break;
          }

        }

        bw.flush();
        bw.close();
        if (to.length() == 0) {
          to.delete();
        }
        return rowCnt;
      } catch (Exception e) {
        throw new RuntimeException("Export to file failed, details are: " + e.getMessage());
      } finally {
        try {
          bw.close();
        } catch (IOException e) {
          throw new RuntimeException("Export to file failed, details are: " + e.getMessage());
        }
        return 0L;
      }
    }

    return 0L;
  }

  private Connection getConnection(String driver, String url, String user, String pwd) {
    try {
      Class.forName(driver);
      return DriverManager.getConnection(url, user, pwd);
    } catch (Exception e) {
      throw new RuntimeException(
          "Can not create jdbc connection with URL '" + url + "', please check details: " + e
              .getMessage());
    }
  }

  @SuppressWarnings("unused")
  private List<String> getCols(ResultSet rs) {
    List<String> cols = new LinkedList<String>();
    try {
      for (int i = 1; i < rs.getMetaData().getColumnCount(); i++) {
        cols.add(rs.getMetaData().getColumnName(i));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Loop columns failed, details are: " + e.getMessage());
    }
    return cols;
  }
}
