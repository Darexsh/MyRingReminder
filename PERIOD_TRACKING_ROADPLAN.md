## Period Tracking Roadplan

Status legend: `[ ]` pending, `[-]` in progress, `[x]` done

### Phase 0: Alignment
- [x] Feature scope agreed (simple period tracking, no extra categories)
- [x] Data strategy agreed (one record per day)
- [x] UX direction agreed (calendar tap -> modal -> save -> immediate update)

### Phase 1: Data Foundation
- [x] Define `PeriodDayEntry` model (date, intensity, pain, illness, start, end, updatedAt)
- [x] Add persistence methods in repository/settings layer
- [x] Add read/write/update/delete API for one day
- [x] Integrate with backup/restore payload
- [x] Add migration/default handling for existing users
- [x] Verify data-only behavior with local tests/manual checks

### Phase 2: Entry Modal (Calendar Input)
- [x] Add calendar day click handler to open period modal
- [x] Build modal UI (period toggle, intensity, pain, illness, start/end)
- [x] Prefill modal for existing entries
- [x] Add `Clear entry` action
- [x] Implement validation (intensity required when period day is on)
- [x] Save flow + immediate state refresh
- [x] Restrict modal opening to ring-free context
- [x] Add hysteresis window: allow entries in ring-free week ±4 days
- [x] Auto-suggest first start marker in a window (without auto-enabling period day)
- [x] Enforce marker mutual exclusion in UI (start vs. end)
- [x] Enforce single start marker per ring-free window (start option disabled on other days)
- [x] Redesign modal UI (custom layout, rounded corners, improved action row)
- [x] Add localized save/delete/validation messages (EN + DE)

### Phase 3: Calendar Visualization
- [x] Add intensity-based red coloring (light/medium/heavy)
- [x] Add blood icon for period days
- [x] Add pain icon conditionally
- [x] Add illness icon conditionally
- [x] Add start/end mini markers (`S` / `E`)
- [x] Resolve visual priority/layout for multiple indicators

### Phase 4: Day Detail View
- [ ] Extend selected-day detail output
- [ ] Show: period yes/no, intensity, start/end, pain, illness
- [ ] Add empty state for days without data
- [ ] Ensure detail updates instantly after save/clear

### Phase 5: QA and Regression Safety
- [ ] Edge-case checks (start=end same day, toggling period off, past/future dates)
- [ ] Confirm no breakage in existing cycle calendar logic
- [ ] Confirm reminders/notifications unaffected
- [ ] Confirm widgets unaffected
- [ ] Backup/restore round-trip verification
- [ ] Final UX cleanup for uncluttered layout
- [ ] Welcome Tour implementation of all new features

### Phase 6: Release Readiness
- [ ] Update README/help text for new feature
- [ ] Add/update screenshots (if needed)
- [ ] Final acceptance criteria pass against your checklist
