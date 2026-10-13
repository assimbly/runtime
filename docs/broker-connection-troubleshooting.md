# Flows stop working: "Could not refresh JMS Connection" / "Connection refused"

**What you see:** flows that pass messages between each other through a queue stop running. The integration log
repeats `Could not refresh JMS Connection ... Could not connect to broker URL ... Connection refused`.

**What it means:** the integration (Camel) cannot reach the message broker. It keeps retrying every 5 seconds
and recovers by itself once the broker accepts connections again. So the question is always: *why is the broker not accepting connections?*

Do the three checks below **in this order**, and **do not restart anything until you have saved the evidence**
(a restart wipes the clues).

---

## Check 1 - Is the broker running?

Ask the broker for its status (replace `<id>` with the broker id used by the gateway):

```
GET /api/brokers/<id>/status?brokerType=classic
```

| Answer | Meaning | Next step |
|---|---|---|
| `started` | Broker thinks it is fine | Go to Check 3 |
| `stopped` | The broker shut itself down (the container is still "up", so nothing restarted it) | Go to Check 2, then restart it |

If it is `stopped`:

1. **Save the broker log first** (copy the whole file).
2. Search the log for the **first** `ERROR` or `WARN` that appears *before* the first `BrokerStoppedException`.
   That earlier message is the real cause. `BrokerStoppedException` itself is only a follow-up error.
   Useful words to search for: `IOException`, `IOExceptionHandler`, `Journal`, `Checkpoint`, `lock`, `Too many open files`, `OutOfMemory`.
3. Restart the broker:

```
GET /api/brokers/<id>/restart?brokerType=classic
```

The integration reconnects by itself within about 5 seconds. If it does **not**, restart the integration too and
report that (it means the problem is on the integration side).

## Check 2 - Is the disk of the broker OK?

The broker stores messages on disk (`/data/.assimbly/broker/data`). Most "broker stopped by itself" cases start with a disk problem.

Ask someone with server access to check, on the broker server/container:

- **Free space:** `df -h /data` - is it (almost) 100% full?
- **Read-only or errors:** `dmesg | tail` or the host/volume events - look for `Read-only file system`, `Input/output error` or `Stale file handle`.
- **Is the disk network-based?** (NFS / SMB / shared storage). This is a known cause of random failures; the broker needs a local or block disk.

Write down what you find and add it to the incident report.

## Check 3 - Is the broker full, or out of connections? (status says `started` but nothing connects)

The broker can be "running" and still refuse new clients. Check:

- **Number of connections:** the broker allows at most **5000**. Look at `currentConnections` in the broker info
  (`GET /api/brokers/<id>/info?brokerType=classic`) or on the broker page. A number close to 5000 means connections are leaking
  and the broker rejects new ones (the broker log then has a WARN: `Exceeded the maximum number of allowed client connections`).
- **Memory / storage usage:** if the broker reports store, memory or temp usage near 100%, it slows down or blocks senders.
- **Is the process stuck?** Heavy memory use or very long pauses make the broker look alive but unresponsive.
  Ask someone with server access for a **thread dump and the memory usage** *before* restarting.

**Tip to tell where the problem is:** after you restart *only the broker*, do the flows come back by themselves?

- **Yes:** the problem was in the broker (Checks 1-3).
- **No, only restarting the integration helps:** the problem is in the integration's connection. Report it together with the log and a thread dump of the integration.

---

## Always send with the incident report

- Time the problem started (the first `Connection refused` in the integration log) and time it was solved.
- The broker log and the integration log for that period.
- The answers to Checks 1-3 (status, disk, number of connections).
- What you did to fix it (which restart, in which order).
