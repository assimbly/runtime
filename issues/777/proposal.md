# Issue #777 — Basic authentication header not accepted by inbound https step

Issue: https://github.com/assimbly/runtime/issues/777

Status: **Proposal updated from supplied DIL version 16. Path defects confirmed; the deployed 401 cause remains unverified.**

## Desired behavior

The reported POST to `https://next.dovetail.world/test/inbound_http/regressiontests/CB_InboundHttpsBasicAuthentication`, with the issue's Basic header, should enter the Regression Tests flow and return `Test succeeded`. Missing, incorrect, or malformed credentials must still be rejected with HTTP 401, without executing the flow or returning HTTP 500. Other flows sharing the listener must retain their own authentication behavior.

## Confirmed findings

- Inspected runtime at `7a3c9847` on `next`. Gateway and custom-components are also clean and on `next`; their remotes match the Assimbly repositories. No applicable AGENTS.md files were found. No branch switch was needed.
- The issue is open, has no labels or comments, and reports a Jetty 401 for the backend path `/regressiontests/CB_InboundHttpsBasicAuthentication` on port 9001. The user subsequently supplied DIL version 16 for flow `6a7f20bcee15e2000c000445`. The response challenge header and deployed revision remain unavailable.
- The supplied Base64 decodes successfully to `fruit:pear`. In `dil/src/main/java/org/assimbly/dil/blocks/connections/auth/BasicAuthentication.java`, `setFields()` reads the connection username, password, and path; `setHandlers()` creates a UserStore, HashLoginService, and ConstraintSecurityHandler, protecting `path + "/*"`. `addToRegistry()` binds the handler under the connection ID.
- `dil/src/main/java/org/assimbly/dil/blocks/connections/Connection.java` decrypts connection properties and starts BasicAuthentication for type `basic`. The marshalling class `dil/src/main/java/org/assimbly/dil/transpiler/marshalling/core/Connection.java` copies configured connection keys into those properties.
- `dil/src/main/resources/kamelets/https-source.kamelet.yaml` forwards the configured `handlers` reference to the Jetty HTTPS consumer. Header removal occurs inside the route, after Jetty authentication, so that removal does not explain this pre-route rejection.
- Commit `c6dce174` adds handling for malformed Base64. Valid Base64 is passed unchanged to Jetty; removing that guard is not supported by the evidence.
- BasicAuthentication constructs a new handler on every start. Inspection of the locally cached Camel 4.22.1 bytecode confirms `JettyHttpComponent.addJettyHandlers()` wraps the server's existing handler and deduplicates by handler equality. Existing `MutualTlsHandler.java` documents retained handlers and reuses instances for this reason. This supports a stale-handler hypothesis, but does not prove it caused this incident.
- The supplied source URI is `https://0.0.0.0:9001/regressiontests/`, with `matchOnUriPrefix=false`. Its handler reference and connection block ID both match `0e640f74-98d4-4389-909c-e4f1a5703967_basic`; there is no reference mismatch. The connection username is `fruit`, and its path is `/regressiontests/`. Its encrypted password cannot be verified as `pear` from the export alone; the ciphertext is intentionally not copied here.
- That connection path produces `/regressiontests//*`. A standalone local Jetty probe confirms this pattern does not protect the reported URL. Both absent and wrong credentials passed the handler. Normalizing it to `/regressiontests/*` produced 401 for absent/wrong credentials and 200 for valid credentials.
- `Unmarshall.setRouteTemplate()` splits the source URI into scheme/path; `RouteTemplate.createTemplateParameters()` passes the path unchanged to `https-source.kamelet.yaml`. This code does not append the flow name. With prefix matching disabled, the supplied endpoint does not match the longer URL in the issue.
- No Basic authentication test source was found locally. No evidence currently requires source changes in gateway or custom-components.

## Diagnosis and minimal change recommendation

1. Correct the regression-flow configuration to match the issue's intended URL: source URI `https://0.0.0.0:9001/regressiontests/CB_InboundHttpsBasicAuthentication`, keeping `matchOnUriPrefix=false`; connection path `/regressiontests/CB_InboundHttpsBasicAuthentication`. Keep the existing matching handler/block references. This also avoids applying this flow's credentials to the entire tenant prefix. This is a proposed configuration edit, not an applied deployment change.
2. In `dil/src/main/java/org/assimbly/dil/blocks/connections/auth/BasicAuthentication.java`, normalize trailing slashes before appending `/*`. Preserve the existing prefix-constraint contract and handle an explicitly configured root `/` as `/*`. Reject missing/blank paths rather than accidentally opening access or protecting every flow. Do not collapse internal slashes or otherwise rewrite URL semantics. This fixes the demonstrated authentication-mapping defect with a small production change and no new dependencies.
3. Add focused regression coverage in `dil/src/test/java/org/assimbly/dil/blocks/connections/auth/BasicAuthenticationTest.java`, exercising the real handler registered by `start()`. Preserve the existing malformed-Base64 guard and Jetty login/role/challenge logic.

These changes correct confirmed defects, but **path normalization alone is not a demonstrated fix for the reported 401**: the malformed mapping bypassed authentication in the probe. Before calling #777 resolved, repeat the corrected flow on the deployed dependency versions and capture the `WWW-Authenticate` realm if it still fails. Check the decrypted credential in the owning environment without logging it, and identify all matching handlers on port 9001.

Keep handler lifecycle changes out of the initial patch unless a restart test reproduces stale credentials. If reproduced, reuse the handler within its owning Camel context/connection lifecycle and safely update the user store instead of rebinding a newly constructed wrapper. Verify removed usernames, path updates, shutdown, and context isolation. A global cache or a new custom authentication implementation is not justified by the current evidence.

## Focused verification

Proposed, not executed:

1. Add the proposed BasicAuthentication test with a real local Jetty/Camel consumer: matching credentials succeed; absent, wrong, and malformed credentials produce 401 and never reach the route. Cover configured paths with/without trailing slashes, root, and invalid missing/blank paths.
2. Exercise two protected paths with different credentials and an unprotected sibling on one port. Check exact and descendant paths against the configured constraint contract.
3. Restart/reconfigure the protected flow while another flow keeps the connector alive. New credentials must work, old credentials must fail, and handler instances must not accumulate. Verify separate Camel contexts remain isolated.
4. Run the focused test with the repository's Java 25 Maven setup, then repeat the issue's regression case through the deployed HTTPS proxy and directly against the backend in an appropriate test environment.

Actually performed: source/history inspection, Git status and branch checks, Base64 decoding, dependency bytecode inspection, GitHub state verification, and a standalone Java 25 probe using Jetty 12.1.9 LocalConnector, ConstraintSecurityHandler, HashLoginService, and synthetic `fruit:pear` credentials. The probe uses an in-process connector and a terminal 200 handler; it does not test Camel route selection or the deployed password.

| Constraint | No header | Correct credentials | Wrong credentials |
| --- | --- | --- | --- |
| `/regressiontests//*` | 200 | 200 | 200 |
| `/regressiontests/*` | 401 | 200 | 401 |

All six requests used `/regressiontests/CB_InboundHttpsBasicAuthentication`. Probe source is at `%TEMP%/assimbly-777-probe/AuthProbe.java (execution sandbox temp directory)` (temporary diagnostic artifact, not a repository test). No full application tests, live regression requests, or implementation changes were made. The POM declares Camel 4.22.1 and Jetty 12.1.13; cached Jetty bytecode inspected was 12.1.12 and the executable probe used a consistent cached 12.1.9 set. Repeat on the actual deployed version before release.

Reference: [Jetty BasicAuthenticator source](https://raw.githubusercontent.com/jetty/jetty.project/jetty-12.1.x/jetty-core/jetty-security/src/main/java/org/eclipse/jetty/security/authentication/BasicAuthenticator.java) confirms the normal decode/login/challenge sequence; this moving branch is supporting context, not proof of the deployed artifact.

## Outstanding information

The supplied DIL resolves the configuration question. Assume the issue's full URL is the intended endpoint and the export reflects the relevant flow version. The encrypted password's plaintext, deployed revision, challenge realm, and live handler chain remain unverified. These are verification requirements for diagnosing any remaining 401, not evidence supporting a speculative lifecycle rewrite. No further user answer is needed to make the confirmed path corrections reviewable.

## GitHub workflow state

Verified on 2026-10-06: `skin27` added as assignee; existing assignee `brunovg` preserved. Backend project status was already **In progress** (option `47fc9ee4`) and was read back unchanged. The connector denied assignment, but the authenticated GitHub CLI completed and verified it. No outstanding GitHub update blocker; no issue comments or pull requests created.
