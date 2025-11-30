# Version Information

## Current Version: v1.0.2

### Changes in v1.0.2

This version includes the following updates:

#### New Features
1. **RealTime Toggle Button in Dashboard**
   - Added "RealTime Check" button to the dashboard UI
   - Toggle button switches between "Start RealTime Check" (orange) and "Stop RealTime Check" (red)
   - Status indicator shows "RealTime mode: Active/Inactive"
   - Dashboard auto-refreshes every 60 seconds when RealTime mode is active

2. **New Backend Endpoint**
   - `/realtime/toggle?player=<playerName>` - Toggle real-time monitoring on/off
   - Returns JSON with success status, active state, and player name

3. **State Persistence**
   - RealTime state persists across page refreshes
   - Button and status indicator automatically restore on page load

#### Bug Fixes
- Fixed compilation error in DashboardServer.java (line 490)
- Corrected string concatenation syntax for JavaScript function declaration

#### Technical Details
- Version defined in: `src/UpdateManager.java`
- Git tag: `v1.0.2`
- Commit: Automatic version bump to v1.0.2

### Git Tag Information

The version tag `v1.0.2` has been created and is ready to be pushed to the remote repository.

**To complete the release:**
```bash
git push
git push origin v1.0.2
```

**Note:** The `.github/workflows/release.yml` is configured to trigger on tag push and will:
1. Build the project
2. Create a release zip file
3. Create a GitHub release with the zip file attached

The release workflow does NOT create its own tags - it uses the tag created here.

### Version Update Process

1. ✅ Read current version from `src/UpdateManager.java` (was: 1.0.0)
2. ✅ Incremented patch version (now: 1.0.2)
3. ✅ Updated version in source code
4. ✅ Committed changes with message: "Automatic version bump to v1.0.2"
5. ✅ Created git tag: v1.0.2
6. ⏳ Ready to push: `git push && git push origin v1.0.2`

### UpdateChecker Compatibility

The UpdateManager will correctly detect this version because:
- Local version: Read from `UpdateManager.VERSION` constant (1.0.2)
- Remote version: Read from GitHub releases API (tag v1.0.2)
- Version comparison: Uses semantic versioning (major.minor.patch)

When a new release is created on GitHub with tag v1.0.2, the UpdateChecker will:
1. Compare local version (1.0.2) with latest release tag (v1.0.2)
2. Determine they match (no update available)
3. When v1.0.3 is released, it will detect the update is available

### Next Steps

To complete the release process:

1. **Push to Remote:**
   ```bash
   git push
   git push origin v1.0.2
   ```

2. **GitHub Actions:**
   - Workflow will trigger on tag push
   - Build will compile the Java project
   - Release will be created automatically
   - release.zip will be attached to the release

3. **Users:**
   - Can run `checkupdates` command to detect new version
   - UpdateManager will download and apply the update automatically
   - Application will restart with new version

### Files Modified

**Version Update:**
- `src/UpdateManager.java` - Version changed from 1.0.0 to 1.0.2

**Feature Implementation:**
- `src/DashboardServer.java` - Added toggle endpoint and UI

**Documentation:**
- `README.md` - Updated with real-time features
- `FEATURES.md` - Added real-time monitoring documentation
