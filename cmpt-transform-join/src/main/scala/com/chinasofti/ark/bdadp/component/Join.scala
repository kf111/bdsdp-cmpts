package com.chinasofti.ark.bdadp.component

import java.util

import com.chinasofti.ark.bdadp.component.api.Configureable
import com.chinasofti.ark.bdadp.component.api.data.{Builder, SparkData}
import com.chinasofti.ark.bdadp.component.api.transforms.MultiTransComponent
import com.chinasofti.ark.bdadp.util.common.StringUtils
import org.slf4j.Logger

import scala.collection.JavaConversions._

/**
  * Created by Hu on 2017/1/12.
  */
class Join(id: String, name: String, log: Logger)
  extends MultiTransComponent[util.Collection[SparkData], SparkData](id, name, log) with
    Configureable {

  var conditionExpr: String = null

  override def apply(inputT: util.Collection[SparkData]): SparkData = {
    Builder.build(inputT.map(_.getRawData).reduce((f, s) => f.join(s, conditionExpr)))
  }

  override def configure(componentProps: ComponentProps): Unit = {
    conditionExpr = componentProps.getString("conditionExpr")
    StringUtils.assertIsBlank(conditionExpr)
  }
}
