Act as an experienced product manager.

Help me draft an initial Product Requirements Document (PRD) for
**MiseWorks — Restaurant Operations Platform**.

This document establishes the product direction and MVP boundaries.
Detailed requirements, user stories, acceptance criteria, architecture,
and implementation planning will follow separately.

## Project context and scope principle

MiseWorks is a hobby project, and I am using it to practice agentic
development. There is no fixed delivery deadline.

This describes the development context, not the expected functional
completeness of the product.

Define the MVP as the smallest coherent, usable restaurant application:
- Include features necessary for the agreed users to complete their
  core workflows.
- Do not exclude necessary features merely to save development time
  or because this is a hobby project.
- Do not add features merely because there is no deadline.
- Assess exclusions by whether users can still operate the application
  without developer intervention or unreasonable workarounds.
- Do not assume a portfolio demonstration is the primary objective.
- An actual restaurant pilot is not required for the first release.
- Do not claim customer validation or production readiness without evidence.

Keep these development motivations out of the product PRD unless they
are necessary to explain a material constraint.

## Working process

1. Read the brainstorming summary and decision record below.
2. Read `docs/PRD.md` if it already exists.
3. Review relevant answers already given in this conversation.
4. Preserve explicitly confirmed decisions unless I revise them.
5. Do not treat previous assistant recommendations, proposed defaults,
   or unanswered questions as approved decisions.
6. If an earlier answer relied on treating the application as a limited
   demonstration, revisit that answer under the scope principle above.
7. Identify missing decisions that materially affect product scope.
8. Ask clarifying questions ONE AT A TIME, starting with the question
   that has the greatest impact on scope.
9. Do not ask again about clearly confirmed decisions.
10. After each answer, ask the next necessary question. Do not generate
    the PRD after the first answer if important gaps remain.
11. If I defer a decision, record it under Open Questions and continue.
12. Ask only questions needed for this initial product document.
    Leave detailed behavior, acceptance criteria, and implementation
    decisions for subsequent requirements work.
13. Once the important questions are answered or explicitly deferred,
    write the complete PRD and save it as `docs/PRD.md`.
14. Do not scaffold applications, install dependencies, implement code,
    commit, or push as part of this task.

## Required document structure

Use exactly these nine top-level sections, in this order:

1. Overview
2. Problem Statement
3. Goal / Vision
4. Target Users
5. Key Features (MVP)
6. Out of Scope
7. Success Metrics
8. Risks and Assumptions
9. Open Questions

## Writing rules

- Write in Markdown.
- Keep the document concise: approximately 600–900 words maximum.
- Use plain language and avoid marketing language.
- Keep Key Features (MVP) to a short list, one line per feature.
- Do not include technical details, architecture, frameworks,
  infrastructure, repository structure, or technology versions.
- Do not write detailed requirements, user stories, API specifications,
  data models, or acceptance criteria yet.
- Do not invent features, customer research, user demand, adoption
  figures, commercial commitments, or numerical success targets.
- Clearly distinguish confirmed decisions from assumptions.
- Put unresolved decisions under Open Questions instead of guessing.
- Use product outcomes for Success Metrics, not repository completion,
  portfolio presentation, or development activity.
- Where a useful success measure lacks an agreed target, state that
  its target is not yet defined rather than inventing one.
- Do not assume an MVP exclusion is a permanent exclusion.
- If my latest answer changes an earlier decision, use the latest
  answer consistently throughout the document.
- The summary contains context and candidate decisions. It is not
  permission to include every mentioned feature.

## Brainstorming summary

### Product identity

**MiseWorks — Restaurant Operations Platform**

The name is inspired by “mise en place”: preparing and organizing
everything needed for restaurant service.

### What we are going to build

A staff-facing application for restaurant operations, initially focused
on reservations, table usage, and menu management.

The core workflow discussed is:

A staff member creates a reservation, assigns a suitable table,
seats the guests, and releases the table when their visit ends.

Walk-in guests should also be supported without requiring an advance
reservation.

Staff can maintain menu information and mark dishes unavailable.

The functional areas discussed are:
- Reservations and table operations.
- Menu management.
- Reservation-related notifications.
- Restaurant configuration and staff access management, subject to
  confirmation of their initial scope.

### Origin of the idea

The idea came from a freelance job posting describing an existing
restaurant platform that needed reservation APIs, table-management
screens, menu editing, recipe management, and booking integration.

That posting is inspiration, not validated customer research and not
a commitment to reproduce the client's entire platform.

Commercial demand has not been validated.

### Problem hypothesis

Restaurant staff need coordinated access to reservations, table status,
and menu availability during service.

Conflicting bookings and outdated operational information are plausible
problems the application could address.

These are hypotheses, not findings from completed customer interviews.

### Goal / vision

Help restaurant staff coordinate bookings, tables, and menu availability
through a coherent operational application.

Establish a usable first release with clear boundaries, while leaving
room for future restaurant operations capabilities.

### Target users

Restaurant employees, especially:
- Managers responsible for restaurant configuration, menus, and staff.
- Staff responsible for reservations, seating, and table status.

Precise permissions and the intended restaurant operating model should
be clarified where they affect the MVP.

A public customer-facing booking application is not confirmed.

## Decision record and candidate scope

The items below reflect the discussion so far.

If an item is marked “confirm,” check whether I already explicitly
answered it in this Codex conversation. If so, use that answer.
Otherwise, ask about it when it materially affects the PRD.

### Restaurant scope — confirm

The latest recommendation was:
- One restaurant at one location for the first release.
- Multiple independent restaurant businesses and multiple locations
  deferred.

This was a recommendation, not automatically an approved decision.

Do not introduce multi-tenancy solely as a portfolio exercise.

### Restaurant setup and staff access — revisit and confirm

An earlier proposal used only preconfigured restaurant and user accounts.
That proposal was influenced by a demonstration-oriented interpretation
and must not be treated as a fixed constraint.

The revised proposed direction is:
- Provision the first manager during initial setup.
- Let the manager configure the restaurant.
- Let the manager add staff, assign roles, and deactivate staff access.
- Normal account administration should not require a developer to
  modify seed data.
- Public self-registration is a separate decision and is not implied.
- Demo data and demo accounts may be provided for convenience.
- An actual restaurant pilot is not a release requirement.

Clarify the necessary product scope without deciding implementation
details such as identity providers or invitation mechanisms prematurely.

### Reservations — confirm workflow boundaries

Capabilities discussed include:
- Create and amend reservations.
- Cancel reservations.
- Assign a suitable table.
- Seat guests.
- Complete visits and release tables.
- Mark no-shows.
- Prevent overlapping bookings for the same table.

Confirm the lifecycle scope if it has not already been answered.

One table per reservation, table combinations, automatic allocation,
and booking-duration settings remain decisions unless already confirmed.

### Walk-ins — use prior answers where confirmed

The proposed behavior discussed is:
- Staff can seat guests without an advance reservation.
- Record party size, assigned table, and seating time.
- Customer details can be optional.
- Suggest an expected end time using a default visit duration.
- Allow staff to adjust the expected end time.
- Check for conflicts with upcoming reservations before seating.
- Keep the table occupied until staff explicitly complete the visit.
- An expected end time must not automatically release the table.
- Flag overdue occupancy and conflicts with upcoming reservations.
- Staff resolve conflicts manually, for example by reassigning an
  upcoming reservation to a suitable table or waiting for release.
- Do not automatically move or cancel bookings.

These are prior recommendations. Treat them as confirmed only where
I explicitly accepted them in this conversation.

### Menu management — use prior answers where confirmed

The proposed initial scope is:
- Categories.
- Dishes.
- Prices.
- Descriptions.
- Availability.

The proposed permission split is:
- Managers maintain menu content and prices.
- Managers and staff can change dish availability.

Recipe management and ingredient costing were proposed for deferral.

Use explicit answers already given; do not treat these recommendations
as approved without confirmation.

### Notifications — unresolved unless already answered

Reservation confirmations and cancellations were discussed.

Clarify:
- Whether notifications are necessary for the initial user workflows.
- Who receives them.
- Whether actual delivery is required for the first release.

Do not assume simulated delivery is sufficient merely because this is
a hobby project. Equally, do not assume external delivery is required
without a product reason.

### External booking integration — unresolved unless already answered

The original inspiration included a third-party booking widget.

Clarify whether the first release needs:
- A real integration.
- A simulated integration for development and verification.
- No external booking integration yet.

No provider has been selected or promised.

### Candidate exclusions — confirm boundaries

The following were suggested for deferral:
- Payments and point of sale.
- Inventory and stock deduction.
- Recipe management and ingredient costing.
- Kitchen order management.
- Delivery operations.
- Advanced analytics.
- Public customer booking interface.
- Multiple restaurant locations.
- Public restaurant registration.

Do not silently exclude a capability if an agreed core workflow
depends on it. Do not add these capabilities without a clear need.

## Candidate success measures

Discuss appropriate ways to assess whether:
- Staff can complete reservation and walk-in seating workflows.
- Conflicting table allocations are prevented.
- Occupancy remains accurate when visits run late.
- Menu information and availability can be kept current.
- Managers can perform agreed restaurant and staff administration
  without developer intervention.

These are candidate areas for measurement, not approved targets,
acceptance criteria, or claims of measured results.

## Start now

Review the existing conversation and this updated scope principle.

Ask the single most important unanswered product-scope question.
If a previous scope answer needs revisiting because it relied on a
demonstration-only interpretation, address that first.

Do not generate the PRD yet.
