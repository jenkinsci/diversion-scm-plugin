# Testing Guide for Modern Credentials API Pattern

## Overview
This guide helps you test that the plugin correctly uses the modern Credentials API pattern (passing `null` for Authentication) and that Jenkins resolves security context internally.

## Test Scenarios

### 1. Pipeline Job Configuration (Item Context)
**Test:** Credentials dropdown in a Pipeline job configuration

**Steps:**
1. Go to Jenkins → New Item
2. Create a Pipeline job (e.g., "test-diversion-pipeline")
3. In Pipeline section, select "Pipeline script from SCM"
4. Select "Diversion" as SCM
5. Click on the "Credentials" dropdown

**Expected Results:**
- ✅ Dropdown populates with available Secret Text credentials
- ✅ Credentials show ID and description (if available)
- ✅ No errors in browser console or Jenkins logs
- ✅ Dropdown works without requiring page refresh

**What to Check:**
- Credentials visible based on your user permissions
- If you're an admin, you should see all credentials
- If you're a regular user, you should only see credentials you have access to

### 2. Global Pipeline Library Configuration (System Context)
**Test:** Credentials dropdown in Global Pipeline Library settings

**Steps:**
1. Go to Jenkins → Manage Jenkins → System
2. Scroll to "Global Pipeline Libraries"
3. Click "Add" to create a new library
4. Select "Diversion" as Modern SCM
5. Click on the "Credentials" dropdown

**Expected Results:**
- ✅ Dropdown populates with available Secret Text credentials
- ✅ Works correctly even though there's no Item context (context is null)
- ✅ Only admins should see credentials (permission check: ADMINISTER)

**What to Check:**
- As admin: Should see all credentials
- As non-admin: Should see empty dropdown (permission denied, but no error)

### 3. Permission Testing
**Test:** Verify permission checks work correctly

**Steps:**
1. Create a test user with limited permissions
2. Log in as that user
3. Try to configure a Pipeline job with Diversion SCM
4. Check the credentials dropdown

**Expected Results:**
- ✅ User without `USE_ITEM` permission: Empty dropdown (no error)
- ✅ User with `USE_ITEM` permission: Sees credentials they have access to
- ✅ No stack traces or errors in Jenkins logs

### 4. Repository/Branch Dropdowns (Cascading)
**Test:** Verify cascading dropdowns work with new authentication pattern

**Steps:**
1. Configure a Pipeline job with Diversion SCM
2. Select a credential from the dropdown
3. Wait for "Repository" dropdown to populate
4. Select a repository
5. Wait for "Branch" dropdown to populate

**Expected Results:**
- ✅ Each dropdown populates after selecting the previous one
- ✅ No authentication errors
- ✅ API calls succeed using the selected credential

### 5. Log Verification
**Test:** Verify no authentication-related errors in logs

**Steps:**
1. Enable debug logging for the plugin:
   - Go to Manage Jenkins → System Log
   - Add new log recorder for `io.superstudios.plugins.diversion`
   - Set log level to `FINE`
2. Perform the UI tests above
3. Check the log recorder

**What to Look For:**
- ✅ No errors about "Authentication" or "ACL.SYSTEM"
- ✅ No type mismatch errors
- ✅ Credentials lookup succeeds
- ✅ API calls to Diversion succeed

### 6. Browser Console Testing
**Test:** Check for JavaScript errors

**Steps:**
1. Open browser developer tools (F12)
2. Go to Console tab
3. Perform the UI tests above
4. Watch for errors

**Expected Results:**
- ✅ No JavaScript errors
- ✅ AJAX calls to `doFillCredentialsIdItems` succeed (200 OK)
- ✅ Dropdowns populate correctly

### 7. Direct API Testing (Advanced)
**Test:** Verify the Stapler endpoints work correctly

**Steps:**
1. Use browser developer tools → Network tab
2. Configure a Pipeline job with Diversion SCM
3. Select credentials dropdown
4. Find the AJAX request to `doFillCredentialsIdItems`
5. Check the response

**Expected Results:**
- ✅ Request uses POST method (CSRF protection)
- ✅ Response contains JSON with credential options
- ✅ Status code: 200 OK

## Verification Checklist

- [ ] Credentials dropdown populates in Pipeline job configuration
- [ ] Credentials dropdown populates in Global Pipeline Library configuration
- [ ] Permission checks work correctly (admin vs regular user)
- [ ] No errors in Jenkins logs
- [ ] No errors in browser console
- [ ] Cascading dropdowns work (Credentials → Repository → Branch)
- [ ] API calls to Diversion succeed
- [ ] No authentication-related warnings or errors

## Troubleshooting

### If credentials dropdown is empty:
1. Check Jenkins logs for errors
2. Verify you have Secret Text credentials configured
3. Verify your user has the correct permissions
4. Check browser console for JavaScript errors

### If you see authentication errors:
1. Check Jenkins logs for stack traces
2. Verify the plugin version matches what you deployed
3. Check that Jenkins restarted after plugin deployment

### If API calls fail:
1. Verify credentials are correct
2. Check network connectivity to Diversion API
3. Check Jenkins logs for API errors
4. Verify the credential ID matches what's configured

## Success Criteria

The modern Credentials API pattern is working correctly if:
- ✅ All dropdowns populate without errors
- ✅ Permission checks work as expected
- ✅ No authentication-related errors in logs
- ✅ The plugin functions identically to before, but uses the modern API pattern internally

