# Static RELEASE preparation evidence

Recorded: `2026-07-27T19:35:18+0800`

## Ownership and repository

- Component: `spring-data-keyvalue-2.7`.
- Change: `release-2-7-18-nes-patch-1` (`spec-driven`, apply-ready).
- Owner lease: `coordinator-wave5-keyvalue-27`, acquired at
  `2026-07-27T11:33:45Z` and expiring at `2026-07-27T17:33:45Z`.
- Branch: `2.7.x-bjca-patch`.
- Preparation base HEAD: `11377b4`.
- `origin`: `https://github.com/atbjca/spring-data-keyvalue.git`.
- `gitlab`: `git@192.168.131.1:NES/spring-data-keyvalue.git`.
- Active OpenSpec change: only `release-2-7-18-nes-patch-1`.
- Build target: Java 8; verification JDK: Amazon Corretto 17.0.17; Maven
  wrapper distribution: 3.9.5.
- Credentials remain exclusively in user-level Maven configuration and were
  neither read nor recorded.

Before RELEASE edits, the tracked worktree was clean. The only untracked
content was this generated release OpenSpec. No unrelated file or local tool
directory is approved for staging.

## Upstream RELEASE gate

The central run manifest records both declared upstreams at `tagged`, which is
past the required `nexus-verified` state:

- Framework `5.3.39-nes.patch.1`, release commit
  `413bca94d5a1190bbb0950164e0cfdabb9cd0a57`.
- Data Commons `2.7.18-nes.patch.1`, release commit
  `370018f58948a064593f11c449ede507f44ef293`.

Data Commons remote verification recorded POM SHA-256
`4e0115ac112e77ed29a0eaa22372287f6d18b388b2276dd38d4ecddf8774d6f1`
and JAR SHA-256
`a09ccbfc32665a33cb4f92ff82e8d101962b9bd96a180cb2e112dd741464f288`.
Its RELEASE-only consumer also resolved Framework `5.3.39-nes.patch.1` with
zero internal SNAPSHOT dependencies.

## RELEASE preparation

The project and all internal dependency properties now use RELEASE versions:

- KeyValue `2.7.18-nes.patch.1`.
- Data Commons `2.7.18-nes.patch.1`.
- Spring Framework `5.3.39-nes.patch.1`.

The root POM has no modules and uses default JAR packaging, so the statically
inferred publication set is one GAV:

`cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue:jar:2.7.18-nes.patch.1`

There are no approved publication exclusions. Active component documentation
now identifies the RELEASE coordinate, RELEASE-only upstream dependencies,
incremental local-install command, Nexus RELEASE destination, and
coordinator-controlled deployment constraint. A scan of `pom.xml`,
`README.adoc`, and `doc/` found no target internal `-SNAPSHOT` version.

The release diff contains only `pom.xml`, component documentation, this
OpenSpec change, and its evidence. Production and test source diffs are empty.

## Reused development validation

The operator explicitly directed the release train not to repeat `make build`
or `make test`. The archived development change records `285/285` tests
passing and a successful install, including the CVE-2026-41719 security
regression. Since the RELEASE diff changes no production source, test source,
or executable build logic, those results are reused under the approved
release-only-diff exception.

This exception does not cover local publication, generated-POM scanning, or
consumer verification. The coordinator must run the incremental
`./mvnw -DskipTests install` command alone in the global build slot before a
release commit is created.

## Local RELEASE publication and consumer

The coordinator ran the following command alone in the global build slot:

```text
./mvnw -DskipTests install
```

It completed successfully without `clean` in Maven `11.344s` (wall clock
`14.70s`). Test execution was skipped, while Maven compiled test sources as
part of the normal install lifecycle.

The complete local publication set contains exactly one GAV and three
versioned assets:

- POM: `bjca-footstone-bpring-data-keyvalue-2.7.18-nes.patch.1.pom` (`4050` bytes).
- Main JAR: `bjca-footstone-bpring-data-keyvalue-2.7.18-nes.patch.1.jar` (`115561` bytes).
- Sources JAR: `bjca-footstone-bpring-data-keyvalue-2.7.18-nes.patch.1-sources.jar` (`85111` bytes).

The structured generated-POM scan reported `filesScanned=1`, `clean=true`,
and `findings=0`. An offline local Maven `dependency:tree` consumer completed
successfully in `1.895s` and resolved KeyValue and Data Commons
`2.7.18-nes.patch.1` plus Framework `5.3.39-nes.patch.1`, with no internal
SNAPSHOT dependency.

An initial sandboxed install attempt reached the install goal but could not
write `~/.m2` (`Operation not permitted`). It did not contact Nexus and is not
a publication failure. The authorized local-repository retry above succeeded.

Post-gate checks confirm production and test source diffs remain empty, the
approved release diff is unchanged, and generated `target/` content is not
staged.

## Release commit and Nexus publication

The dedicated release commit is
`8ad61fd79f900f41ad6e931b4298724f190ae2ed`. Immediately before deployment,
the target POM and JAR both returned HTTP 404 from Nexus RELEASE.

The coordinator executed this command once from the clean release commit:

```text
./mvnw -DskipTests -Dmaven.test.skip=true deploy
```

It omitted `clean`, test compilation, and test execution. The deploy completed
successfully at `2026-07-27T19:57:03+08:00` in Maven `7.321s` (wall clock
`10.55s`) and published the POM, main JAR, sources JAR, and Maven metadata.

Post-deployment verification downloaded the POM and main JAR directly from
Nexus RELEASE. The remote POM declares only RELEASE internal dependencies and
has zero internal SNAPSHOT findings.

- POM URL: `http://192.168.131.36:8088/repository/releases/cn/bjca/footstone/bpring/data/bjca-footstone-bpring-data-keyvalue/2.7.18-nes.patch.1/bjca-footstone-bpring-data-keyvalue-2.7.18-nes.patch.1.pom`
- POM SHA-256: `15e3dbf885460ca22c49cd7e6ca94717cfa7e2459489391530ccb25983d529d2`
- JAR URL: `http://192.168.131.36:8088/repository/releases/cn/bjca/footstone/bpring/data/bjca-footstone-bpring-data-keyvalue/2.7.18-nes.patch.1/bjca-footstone-bpring-data-keyvalue-2.7.18-nes.patch.1.jar`
- JAR SHA-256: `a0c3c4297ea7855e9cb790c66c59b8c856613ae3c8ce160afa38316c519077a6`

A consumer using an isolated local repository and repositories with snapshots
disabled completed in Maven `11.947s`. It downloaded and resolved KeyValue and
Data Commons `2.7.18-nes.patch.1` plus Framework `5.3.39-nes.patch.1`, with no
internal SNAPSHOT. The first sandboxed network attempt could not retrieve Maven
plugins; the authorized network retry succeeded and is the recorded result.

Annotated tag `v2.7.18-nes.patch.1` was created locally after Nexus
verification. Tag object `7257875d06e5dcef0992713aa3a95d22cbddb98d`
peels exactly to the release commit. GitHub commit/tag push and remote
verification remain pending because the known `github.com:443` connectivity
blocker persists; Nexus must not be redeployed when Git is retried.
