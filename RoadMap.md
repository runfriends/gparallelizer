## RoadMap ##

### 0.8.5 ###

Planned release: None



### 0.9 ###

  * Distributed actors (Alex)
  * Rename and move the project to Codehaus (Vaclav)
  * Repackage, rename factory classes (Dierk)
  * Old Dataflow concurrency enhancements, renaming? (Dierk)
  * New DataFlow constructs, operator (Russel)
  * Build automation improvements with Gradle (Vaclav)
    * Provide separate samples, doc and src downloads
  * Documentation on the main concepts. What is _our_ understanding of (Dierk)
    * actors
    * Update for actor-based printing, create a Printer actor
    * dataflow (stream, operator)
    * Safe
    * glossary with a paragraph per concept, maybe even as small as a link to an external definition.
    * Which Java concepts do we work with (pools, executors, ...)
    * Common conventions: << for "bind", ...
  * New logo contest (Paul)
  * Refactored and tuned actors (Vaclav)
  * Unified pooled and thread actors (Vaclav)
  * Bugfixing

Planned release: Fall 2009

### 0.10 ###
  * System messages, failover
  * Further actor improvements

### Backlog ###
  * Actor-based metaClass?
  * Leverage Groovy dynamic dispatch to make actor use more natural
  * CSP
  * Performance optimization
  * Timeout handling enhancements

## Release plan ##

Since we're exploring quite an unknown territory here and still live in the 0.x era we will make our release cycle pretty short (estimated about max 2 months each), preferably a minor release after each completed major addition.
We'll only make bugfix releases in case there's a considerable number of bugs that need to get fixed. This policy is likely to change once we reach the 1.0 era.