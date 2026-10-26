# 1. Overview

MiseWorks — Restaurant Operations Platform is a staff-facing application for one restaurant at one location. Its first release brings reservations, walk-in seating, table occupancy, menu information, and restaurant administration into one application.

This initial PRD records confirmed product direction and MVP boundaries. Detailed requirements and acceptance criteria will follow separately. Customer demand has not been validated, and this document makes no claim of production readiness.

# 2. Problem Statement

Restaurant staff need coordinated information about bookings, available tables, current guests, and dishes that can be offered. Conflicting allocations, visits that run longer than expected, and outdated menu availability can make service harder to coordinate.

These are problem hypotheses rather than findings from customer interviews. MiseWorks will address them through shared operational information and explicit staff control over seating and table release. Managers also need to maintain restaurant settings and staff access without routine developer intervention.

# 3. Goal / Vision

Enable staff to manage a booking from creation through departure, accommodate walk-ins, and keep table occupancy and menu availability current. Enable managers to configure the restaurant and administer staff access independently.

The MVP is the smallest coherent application supporting these workflows. Actual use in a restaurant pilot is not a first-release requirement. Future capabilities may expand the platform, but are not commitments within this scope.

# 4. Target Users

- **Managers:** configure the restaurant, tables, seating capacities, opening/service hours, and default visit duration; manage staff accounts and roles; maintain menu content and prices; oversee service operations.
- **Service staff:** enter and amend reservations, allocate tables, seat guests, handle walk-ins, maintain visit status, resolve seating conflicts, and update dish availability.

Guests are recipients of reservation emails when they provide an email address. They do not use a public booking interface in the MVP. Precise permissions within the agreed manager and staff responsibilities will be defined during detailed requirements work.

# 5. Key Features (MVP)

- Initial manager account setup, restaurant configuration, and manager-controlled staff creation, role assignment, and access deactivation.
- Configurable tables, seating capacities, opening/service hours, and default visit duration, with individual duration adjustments by staff.
- Staff-entered reservations covering creation, amendment, cancellation, suitable table assignment, seating, visit completion, table release, and no-shows.
- Staff-selected single tables or table combinations for reservations and walk-ins, with prevention of overlapping bookings for the same tables.
- Walk-in seating with adjustable expected duration and checks against upcoming reservations.
- Shared occupancy information, explicit staff-controlled table release, and warnings for overruns and conflicts with upcoming reservations.
- Manager-maintained menu categories, dishes, prices, and descriptions, with manual availability updates by managers and staff.
- Actual reservation confirmation and cancellation emails to guests who provide an email address, while supporting reservations without email.

# 6. Out of Scope

The MVP excludes multiple restaurants or locations, public self-registration, public booking interfaces, and external booking integrations. Staff enter all bookings.

Automatic table allocation, reassignment, cancellation, and release are excluded. Expected departure times support planning and warnings; they do not end occupancy. Staff resolve conflicts manually.

Payments/POS, inventory and stock deduction, recipe management and ingredient costing, kitchen order management, delivery operations, and advanced analytics are deferred. Menu availability is maintained manually rather than inferred from consumption. SMS and messaging-app notifications are also deferred. These exclusions apply to the MVP, not necessarily to future releases.

# 7. Success Metrics

Assess success through the following product outcomes; numerical targets are not yet defined:

- Staff completion of reservation and walk-in workflows without unreasonable workarounds.
- Prevention of conflicting table allocations during booking and seating.
- Accuracy of recorded occupancy when visits run late, including visibility of upcoming conflicts.
- Staff ability to keep menu content and dish availability current.
- Manager completion of restaurant configuration and staff administration without developer intervention.
- Delivery of confirmation and cancellation emails for reservations with a supplied email address.

Evidence can initially come from observed workflow evaluations; real restaurant adoption is not a release requirement. Evaluation methods and targets remain to be agreed, and no measured results are claimed.

# 8. Risks and Assumptions

The product assumes staff will record bookings, update availability, and explicitly complete visits promptly. Inaccurate or delayed updates can undermine the shared operational view.

Booking conflict prevention cannot prevent guests from staying longer than expected. Warnings and manual resolution remain necessary. Staff-selected table combinations assume staff can judge which physical tables can suitably serve one party.

Email communication depends on guests supplying usable addresses and successful delivery. Reservations without email still require staff-managed communication outside the application.

The proposed workflows and their fit with actual restaurant practices remain unvalidated. Ordering and billing are assumed to continue outside MiseWorks.

# 9. Open Questions

- What evaluation methods and numerical targets should be used for the agreed success measures?
- What guest information is necessary for reservations and walk-ins, and how long should it be retained?
- Should amendments to existing reservations trigger updated guest emails in addition to confirmations and cancellations?

These questions remain unresolved and should be addressed during subsequent requirements work; they do not imply additional approved MVP features.
