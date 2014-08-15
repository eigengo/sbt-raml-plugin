package org.eigengo.sbtraml.helpers

import com.github.jknack.handlebars.{Options, Helper}

object MarkdownHelper extends Helper[AnyRef] {

  override def apply(context: AnyRef, options: Options): CharSequence = Option(context).map(_.toString).getOrElse("")

}
