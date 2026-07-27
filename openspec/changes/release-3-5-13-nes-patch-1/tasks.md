## 1. Worktree freeze and OpenSpec readiness

- [x] 1.1 Confirm this change is apply-ready and assigned exclusively to `spring-data-keyvalue-3.5` in `spring-data-keyvalue-3.5`.
- [x] 1.2 Acquire the component owner lease and record owner/session/timestamps in the release manifest.
- [x] 1.3 Record branch, `origin` fetch/push URLs, HEAD SHA, active OpenSpec changes, toolchain, current version, and target `3.5.13-nes.patch.1` without exposing credentials.
- [x] 1.4 Inventory tracked and untracked changes; separately resolve any unrelated tracked work and exclude local tool directories from all release staging.
- [x] 1.5 Confirm all declared upstream components are Nexus-verified RELEASEs and record their evidence: `spring-framework-6.2`, `spring-data-commons-3.5`.
- [x] 1.6 Confirm the approved explicit exclusions and how each will be verified: none.

## 2. RELEASE preparation and component documentation

- [x] 2.1 Set the component version to `3.5.13-nes.patch.1`.
- [x] 2.2 Replace every internal `cn.bjca.footstone` SNAPSHOT dependency, parent, imported BOM, and plugin with the approved RELEASE version.
- [x] 2.3 Discover the complete local publication set and reconcile it with the representative GAVs: `cn.bjca.footstone.bpring.data:bjca-footstone-bpring-data-keyvalue:3.5.13-nes.patch.1`.
- [x] 2.4 Configure the release publication to exclude only the approved items and prove included POMs do not depend on excluded modules.
- [x] 2.5 Update `README.adoc`, `doc/QUICK_START.md`, `doc/USER_MANUAL.md`, `doc/GAV_MAPPING.md` with version, GAVs, required RELEASE dependencies, validation commands, Nexus location, exclusions, and notable constraints.
- [x] 2.6 Review the full working diff and confirm it contains no unrelated source changes or credentials.

## 3. Local verification

- [x] 3.1 Audit and reuse the committed build/package evidence under the operator-approved release-only-diff exception; do not rerun `make build`.
- [x] 3.2 Audit and reuse the committed 374-test evidence under the same exception; do not rerun `make test`.
- [x] 3.3 Generate local publications/effective POMs incrementally with `./mvnw -DskipTests install` and without `clean`.
- [x] 3.4 Scan every generated POM/BOM for internal `cn.bjca.footstone` `-SNAPSHOT` references; block on any match.
- [x] 3.5 Run a representative consumer test using only the locally prepared RELEASE publications and approved upstream RELEASEs.
- [x] 3.6 Store command, timestamp, exit status, artifact list, POM scan, and consumer evidence in the approved evidence location.
- [x] 3.7 Re-run worktree and diff checks, then advance the manifest to `locally-verified` only when all gates pass.

## 4. Dedicated release commit

- [x] 4.1 Stage only files covered by this OpenSpec, including required component release documentation.
- [x] 4.2 Inspect the staged diff and verify the target version, internal RELEASE dependencies, exclusions, and absence of unrelated files or secrets.
- [x] 4.3 Create the dedicated release commit for `spring-data-keyvalue-3.5` `3.5.13-nes.patch.1`.
- [x] 4.4 Record the release commit SHA and prove the worktree/build inputs match that commit.
- [x] 4.5 Return the repository and evidence to the coordinating main session; do not deploy, tag, or push from an unapproved child session.

## 5. Nexus absence and coordinator authorization

- [x] 5.1 From the release commit, regenerate or verify the complete publication set.
- [x] 5.2 Query Nexus RELEASE for every discovered target GAV and record immutable absence evidence immediately before deploy.
- [x] 5.3 Block if any target-version POM, binary, checksum, metadata, or partial module asset already exists.
- [x] 5.4 Have the coordinating main session review worktree, OpenSpec, local verification, release commit, upstream, exclusion, credential-isolation, and Nexus-absence evidence.
- [x] 5.5 Preview the exact incremental Maven deploy command without `clean` and record explicit coordinator authorization.

## 6. Single deploy and remote artifact verification

- [x] 6.1 Execute the incremental Maven deployment once from the recorded release commit using explicit coordinator authorization.
- [x] 6.2 Record deploy start/end times and sanitized output without credential values.
- [x] 6.3 Download representative POM and binary/BOM assets from Nexus RELEASE and verify version, expected modules, metadata, and exclusions.
- [x] 6.4 Scan all downloaded POM/BOM metadata for internal SNAPSHOT references.
- [x] 6.5 Record immutable asset URLs and checksums.
- [x] 6.6 Run the RELEASE-only consumer smoke test without an internal SNAPSHOT repository.
- [x] 6.7 Advance to `nexus-verified` only when all remote and consumer gates pass; otherwise record `partial-failure` and do not redeploy or tag this version.

## 7. Annotated tag and controlled Git push

- [x] 7.1 Confirm `v3.5.13-nes.patch.1` did not already exist locally and the target release commit SHA is unchanged; remote check remains blocked by GitHub connectivity.
- [x] 7.2 Create annotated tag `v3.5.13-nes.patch.1` on the exact release commit only after `nexus-verified`.
- [ ] 7.3 Push the approved release commit and annotated tag to `origin` under coordinator control.
- [ ] 7.4 Fetch and independently verify the remote branch SHA, annotated tag object, and peeled target commit.
- [x] 7.5 Preserve verified Nexus state and retry only the Git operation after the known connectivity blocker clears.

## 8. Documentation, reconciliation, and archive

- [x] 8.1 Verify component documentation accurately describes the published RELEASE and does not claim excluded artifacts were published.
- [x] 8.2 Update the central manifest with current state, release commit, local tag target, Nexus URLs, checksums, evidence paths, exclusions, and verification timestamps.
- [ ] 8.3 Reconcile the manifest against component Git, active OpenSpec, remote tag, and Nexus; resolve every mismatch.
- [ ] 8.4 Update the human-readable run report and central project documentation only after artifact and tag verification.
- [x] 8.5 Release the owner lease after all component writes and evidence updates finish.
- [ ] 8.6 Validate this component change and archive it only after state reaches `documented` with complete Git/Nexus/manifest evidence.
- [ ] 8.7 Record the OpenSpec archive path and final `archived` state in the release manifest.
