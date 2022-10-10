# Scripts usages

Scripts in this bin directory can be executed
for Windows from _/bin/win_ or Mac/Linux from _/bin/mac_

---
## build

Builds the project or module.

### Usage:

To build the complete project:

```build```

To build a specific module

```build modulename```

---

## buildandtest

Builds the project or module and then run the unit tests.

### Usage:

To build and test the complete project:

```buildandtest```

To build and test a specific module

```buildandtest modulename```

---

## lazygit

Add all files to the staging area, commit the files and push it to origin (GitHub). Shortcut for:

```
git add -A
git commit -m "<commit message"
git push
```

### Usage:

```lazygit "my message"```

---

## checkversions

Checks the Maven dependencies for the latest versions.

The patterns in the rules.xml are excluded. The rules.xml is
in the root directory of the project.

### Usage:

To print a report:

```checkversions```

---

## updateversion

Update the version number of the Maven project and its submodules.

### Usage:

To print a report:

```updateversion <versionnumber>```

example

```updateversion 1.1.0```