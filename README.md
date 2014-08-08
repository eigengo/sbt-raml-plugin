SBT RAML Plugin
===============

[![Build Status](https://travis-ci.org/eigengo/sbt-raml-plugin.png?branch=master)](https://travis-ci.org/eigengo/sbt-raml-plugin)

The purpose of this plugin is to provide syntax checking and documentation generator for your RAML API definitions.

It exposes two tasks: ``raml:verify`` and ``raml:doc``. The ``verify`` task performs syntax and include resolution checks,
if it succeeds, then your RAML definitions are valid, and can be used in some testing tool. The ``doc`` task takes the
RAML definitions and generates HTML documentations.

Usage
=====

Write the RAML descriptors in ``src/raml``. Your RAML files can ``include`` other RAML files, the verifier and documentation generator follows the naming & directory structure. To include the verifier and documentation generator, add the plugin to your ``project/plugins.sbt`` and then add

```scala
org.eigengo.sbtraml.RamlPlugin.settings
```

to your ``build.sbt``. This will add the RAML verify check to the ``compile`` task and documentation task to the
``publishLocal`` task.

---

Notes:
Both tasks use the ``source in Raml`` setting to point to a directory where the RAML files live. Future versions will
probably need ``stylesheet in Raml`` to indicate the location of a CSS file for the documentation, and ``target in Raml``
to specify the output directory for the HTML documentation.
