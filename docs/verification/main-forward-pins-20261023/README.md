# Coherent imaging source pins

Qualified source `11b547338df58dc8b922006e0a9cca9b575fe3ff` changes only the neurosurf4s and neuroatlas4s default revisions. The surface forward port and atlas pin update are published and qualified separately. Algorithms, APIs, domains, all other pins, fixtures and thresholds remain unchanged.

Executed on JDK 22 with default exact Git sources: surface 3 JVM/3 JS; atlas 7/7; independent public imaging consumer 2/2. The consumer verifies physical surface adjacency, explicit vertex order, atlas-domain correspondence, display order and literal measured values. See [qualification.json](qualification.json) and compressed raw logs.

These 24 test executions do not repeat the earlier generic MVPA numerical qualification and do not establish performance, hosted CI, full downstream closure or a main merge. ScalaFIM checks the final default provider closure downstream. Workspace catalog impact reporting was unavailable due to existing uncataloged repositories/worktrees; actual build edges were inspected.
