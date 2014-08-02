SBT RAML Plugin
===============

The purpose of this plugin is to provide syntax checking and documentation generator for your RAML API definitions.

It exposes two tasks: ``raml:compile`` and ``raml:doc-html``. The ``compile`` task performs syntax and include resolution checks, if it succeeds, then your RAML definitions are valid, and can be used in some testing tool. The ``doc-html`` task takes the RAML definitions and generates HTML documentations. 

---

Notes:
The ``doc-html`` task uses the ``stylesheet in Raml`` to specify a sequence of CSS files that will be used.

The ``compile`` task looks for ``*.raml`` in ``ramlSource in Raml``, by default ``src/test/resources`` directory. 