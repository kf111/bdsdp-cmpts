package com.chinasofti.ark.bdadp.component

import com.chinasofti.ark.bdadp.component.api.Configureable
import com.chinasofti.ark.bdadp.component.api.data.{Builder, SparkData}
import com.chinasofti.ark.bdadp.component.api.transforms.TransformableComponent
import org.apache.spark.sql.DataFrame
import org.slf4j.Logger

/**
  * Created by Administrator on 2017/2/4.
  */
class Select (id: String, name: String, log: Logger)
  extends TransformableComponent[SparkData, SparkData](id, name, log) with Configureable {

  var colExpr: String = null

  override def apply(inputT: SparkData): SparkData = {
    val df: DataFrame = inputT.getRawData
    val res: DataFrame = df.selectExpr(colExpr.split(","): _*)
    Builder.build(res)
  }

  override def configure(componentProps: ComponentProps): Unit = {
    colExpr = componentProps.getString("colExpr");
  }
}