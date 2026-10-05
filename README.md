# Green Cart — Final Enhanced Assignment Build

Spring Boot + Thymeleaf + MySQL grocery ordering system.

## Run

1. Start MySQL 8.x / MySQL Workbench.
2. For a fresh database, run `database/01_mysql_workbench_setup.sql` once.
3. Open the folder containing `pom.xml` in IntelliJ IDEA.
4. Use Java 17.
5. Run `com.greencart.GreenCartApplication`.
6. Open `http://localhost:8080`.

Default database connection:

- Database: `grocery_ordering`
- Username: `root`
- Password: `GreenCart@123`

You can override these with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables.

## Main completed functions

- Product and category CRUD, inventory/stock management, selected product discounts, stock-status badges, image validation, product search/filter/pagination and soft delete.
- Inventory report with low-stock and out-of-stock alerts, stock value, category summary, and inventory audit history.
- Shopping cart CRUD, quantity validation, discount totals, checkout and concurrency-safe stock reservation.
- Strict order workflow: `PENDING -> CONFIRMED -> SHIPPED -> DELIVERED`, with customer/admin cancellation rules.
- Dedicated Delivery Manager role with delivery-staff create/edit/enable/disable, capacity management, manual/least-loaded assignment, reassignment, delayed/rescheduled/failed delivery handling, and multiple simultaneous active deliveries per staff member.
- Supplier CRUD, purchase orders with automatic supplier-product linkage, supplier delivery receiving and supplier performance summary.
- Customer feedback, complaints, support inquiries, website contact messages and management responses.
- Role-based management access for product, order, supplier, feedback and delivery-management staff.
- Optional SMTP password-reset/contact email support; disabled by default for local development.
- System audit logs for inventory, orders, delivery, suppliers and security changes.
- 10-digit phone validation, strong password rules, duplicate product/supplier validation and image type/size validation.
- Functional Contact Us and Store Locator pages.

## Demo logins

### Management
- Admin: `admin@greencart.com` / `Admin@123`
- Business Owner: `owner@greencart.com` / `Owner@123`
- Product Manager: `product@greencart.com` / `Product@123`
- Order Administrator: `orders@greencart.com` / `Orders@123`
- Supplier Manager: `supplier@greencart.com` / `Supplier@123`
- Feedback Administrator: `feedback@greencart.com` / `Feedback@123`
- Delivery Manager: `deliverymanager@greencart.com` / `DeliveryManager@123`

### Customer
- `user@greencart.com` / `User@123`

### Delivery staff
- Delivery-person accounts are not pre-seeded. Create them from **Delivery Management → Add Delivery Staff**.

## Optional SMTP email

Local development uses `MAIL_ENABLED=false`, so the application does not require an SMTP server to start. Password-reset URLs are written to the IntelliJ console when mail is disabled.

To send real email, set these environment variables and restart:

- `MAIL_ENABLED=true`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `MAIL_SUPPORT_TO`
- `MAIL_SMTP_AUTH=true` when required
- `MAIL_STARTTLS=true` when required
- `APP_BASE_URL=http://localhost:8080` or your deployed URL

## Notes

- `spring.jpa.hibernate.ddl-auto=update` is enabled, so an existing `grocery_ordering` database is upgraded with new runtime tables/columns when the application starts.
- `database/01_mysql_workbench_setup.sql` is kept as the clean fresh-database setup script.
- No automated test suite is included in this build, as requested.


## Checkout profile autofill
Customer registration now requires full name, phone number and delivery address. The checkout page automatically loads the logged-in customer's name, email, phone and address from the `users` table. Name and email are displayed read-only; delivery address and phone are pre-filled but can be changed. Any changed address/phone is saved back to the customer profile and becomes the default for the next order.

### Cart quantity auto-update
The cart quantity field now saves automatically. Changing the quantity recalculates the row total, discount savings, delivery fee/free-delivery state, final total, and header cart amount without an extra Update button or page reload.

## Final professional UX / backend additions

- Customer order progress timeline from placed -> confirmed -> assigned -> picked up -> on the way -> delivered.
- Bootstrap toast notifications and reusable confirmation modal for destructive actions.
- Admin search/filter/pagination for products, orders and deliveries.
- Dashboard order-status counters plus low-stock/out-of-stock cards.
- Pessimistic database row locking during checkout to prevent overselling under concurrent orders.
- Strict order and delivery state-transition validation.
- Product and supplier soft-delete behavior to preserve historical references.
- Additional database indexes for common product, order and delivery queries.
- Delivery staff can receive multiple active orders at once up to a configurable capacity (default 5); the dashboard shows active workload counts.


## Latest functional upgrades
- Customer PENDING-order cancellation with cancellation reason, cancellation timestamp, stock restoration and audit history.
- Admin order search by order ID/customer plus status and date-range filters.
- Customer order details and delivered-order Reorder flow.
- Checkout/admin delivery notes visible to delivery staff.
- Delivery staff workload capacity (default 5 active deliveries, configurable 1-20).
- Dashboard charts for monthly orders, monthly revenue, order status and top-selling products.
- Structured audit log old/new values for important status and stock changes.

## Final UI polish upgrade

- Redesigned management dashboard with KPI cards, order pipeline, stock alerts, quick actions and cleaner analytics panels.
- Unified admin table/filter styling with clearer search controls, empty states, pagination and compact action layouts.
- Consistent order status badges and a clearer customer order progress timeline.
- Improved Bootstrap toast notifications and a reusable confirmation modal that works from either forms or submit buttons.
- Redesigned customer Order Details page with timeline, product table, payment summary, shipping details and responsive actions.
- Grouped management sidebar with active-page highlighting and a mobile off-canvas menu with backdrop/escape-to-close behavior.

## Design Patterns Integrated for Final Presentation

The final presentation build contains real, integrated design-pattern implementations under:

`src/main/java/com/greencart/designpattern/`

- Member 1 - Product & Inventory Management: **Observer Pattern** (`member1product/observer`)
- Member 2 - Shopping Cart Management: **Strategy Pattern** (`member2cart/strategy`)
- Member 3 - Order Management: **Observer Pattern** (`member3order/observer`)
- Member 4 - Delivery Management: **Strategy Pattern** (`member4delivery/strategy`)
- Member 5 - Supplier Management: **Factory Pattern** (`member5supplier/factory`)
- Member 6 - Customer Support & Feedback: **Observer Pattern** (`member6support/observer`)

These classes are called by the existing ProductService, CartService, OrderService, DeliveryService,
SupplierManagementController and FeedbackManagementController. They are not isolated demonstration-only samples.


## Site review moderation
- Logged-in customers can rate the overall GreenCart website/service at `/site-reviews`.
- New or edited site reviews start as `PENDING`.
- Feedback administrators can approve/reject them at `/admin/site-reviews`.
- Only `APPROVED` site reviews are rendered on the logged-in customer homepage.
- Order reviews remain separate and still require a delivered order.
