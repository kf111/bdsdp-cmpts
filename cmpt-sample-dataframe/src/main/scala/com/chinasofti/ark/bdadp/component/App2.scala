package com.chinasofti.ark.bdadp.component

import java.util

import com.chinasofti.ark.bdadp.component.api.Configureable
import com.chinasofti.ark.bdadp.component.api.data.SparkData
import com.chinasofti.ark.bdadp.component.api.options.{PipelineOptionsFactory, ScenarioOptions, SparkScenarioOptions}
import com.chinasofti.ark.bdadp.component.api.transforms.MultiTransComponent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.slf4j.LoggerFactory

/**
<<<<<<< HEAD
 * Created by White on 2017/1/3.
 */
=======
  * Created by White on 2017/1/3.
  */
>>>>>>> c5c6e652a6967989a1d0e5a8aa802015dea6fab4
object App2 {

  def main(args: Array[String]) {
    val log = LoggerFactory.getLogger(this.getClass)

    val options = PipelineOptionsFactory.fromArgs(args).as(classOf[ScenarioOptions])

    val input = options.getSettings.getOrDefault("pipeline.input",
<<<<<<< HEAD
                                                  """[{"name": "liu", "age": 18},{"name": "xiao", "age": 25},{"name": "bai", "age": 30}]""")
    val input2 = options.getSettings.getOrDefault("pipeline.input2",
                                                   """[{"name": "liu", "money": 18},{"name": "xiao", "money": 25},{"name": "bai", "money": 30}]""")
    val transform = options.getSettings.getOrDefault("pipeline.transform",
                                                      """[{"id": "1", "name": "join", "conditionExpr": "name"}]""")
=======
      """[{"name": "liu", "age": 18},{"name": "xiao", "age": 25},{"name": "bai", "age": 30}]""")
    val input2 = options.getSettings.getOrDefault("pipeline.input2",
      """[{"name": "liu", "money": 18},{"name": "xiao", "money": 25},{"name": "bai", "money": 30}]""")
    val transform = options.getSettings.getOrDefault("pipeline.transform",
      """[{"id": "1", "name": "join", "conditionExpr": "name"}]""")
>>>>>>> c5c6e652a6967989a1d0e5a8aa802015dea6fab4

    options.setDebug(true)
    options.setScenarioId("1")
    options.setExecutionId("1")

    val json1 = options.as(classOf[SparkScenarioOptions]).sparkContext().parallelize(input :: Nil)
    val json2 = options.as(classOf[SparkScenarioOptions]).sparkContext().parallelize(input2 :: Nil)
    val rawData1 = options.as(classOf[SparkScenarioOptions]).sqlContext().jsonRDD(json1)
    val rawData2 = options.as(classOf[SparkScenarioOptions]).sqlContext().jsonRDD(json2)
    val data1 = new SparkData(rawData1)
    val data2 = new SparkData(rawData2)

    val inputT = new java.util.ArrayList[SparkData]()

    inputT.add(data1)
    inputT.add(data2)

    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    val pipeline = mapper.readValue[Seq[TransformModel]](transform).map(f => {
      val className = Array("com.chinasofti.ark.bdadp.component",
<<<<<<< HEAD
                            f.name.charAt(0).toUpper + f.name.substring(1)).mkString(".")
=======
        f.name.charAt(0).toUpper + f.name.substring(1)).mkString(".")
>>>>>>> c5c6e652a6967989a1d0e5a8aa802015dea6fab4
      val clazz = Class.forName(className)

      val constructor = clazz.getConstructors()(0)
      val obj = constructor.newInstance(f.id, f.name, log)

      val props = new ComponentProps()

      props.setProperty("conditionExpr", f.conditionExpr)

      obj.asInstanceOf[Configureable].configure(props)
      obj.asInstanceOf[MultiTransComponent[util.Collection[SparkData], SparkData]]

    }).map(_.apply(inputT))


    pipeline.head.getRawData.show()

  }

  case class TransformModel(id: String, name: String, conditionExpr: String)

}
