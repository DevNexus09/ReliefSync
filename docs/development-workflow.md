# Two-Developer Workflow

## Team record

Record the two confirmed team members here before the first collaborative feature begins:

| Role label | Name | GitHub account |
|---|---|---|
| Developer 1 | To be confirmed | To be confirmed |
| Developer 2 | To be confirmed | To be confirmed |

## Branch model

- `main` is the stable release branch and contains only reviewed, demonstrable work.
- `develop` is the integration branch for completed work planned for the next release.
- Each task is developed on a short-lived `feature/...`, `fix/...`, `test/...`, or `docs/...` branch.
- A branch should address one coherent task and remain small enough for the other developer to review.
- Feature branches target `develop`; only reviewed release pull requests merge `develop` into `main`.
- Direct feature commits to `develop` or `main` are not allowed.

## Working cycle

1. Agree on the task and its acceptance criteria.
2. Pull the latest `develop`.
3. Create a descriptive branch such as `feature/phase-1-foundation`.
4. Commit focused changes with messages such as `feat: add SQLite connection smoke test`.
5. Run `mvn clean test` locally.
6. Push the branch and open a pull request.
7. The other developer reviews behavior, architecture boundaries, tests, and documentation.
8. Resolve review comments and rerun the tests.
9. Merge into `develop` only after approval and a passing build.
10. Delete the merged feature branch and pull the updated `develop`.

When a release checkpoint is ready, open a separate pull request from `develop` to `main`. Tag the release only after that pull request has passed review and CI.

## Contribution expectations

- Both developers implement features and tests; documentation-only commits are not the sole contribution of either member.
- Pull-request descriptions identify the requirement IDs addressed and the commands used for verification.
- The author does not self-approve their pull request.
- Large generated files, IDE settings, build output, and local databases are never committed.

## Conflict handling

The developer whose branch has the conflict updates it from `develop`, resolves the conflict locally, reruns the complete test suite, and asks the reviewer to inspect the resolution before merge.
