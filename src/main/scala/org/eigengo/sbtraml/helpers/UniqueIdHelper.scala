package org.eigengo.sbtraml.helpers

import java.security.MessageDigest

import com.github.jknack.handlebars.{Options, Helper}
import org.apache.commons.lang3.StringUtils

object UniqueIdHelper extends Helper[AnyRef] {
  private val md = MessageDigest.getInstance("SHA-256")

  override def apply(context: AnyRef, options: Options): CharSequence = context match {
    case null =>
      StringUtils.EMPTY
    case _    =>
      val mdbytes = md.digest(context.toString.getBytes)
      //convert the byte to hex format method 1
      val sb = new StringBuilder
      mdbytes.foreach(b => sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1)))
      sb.toString()
  }
}
