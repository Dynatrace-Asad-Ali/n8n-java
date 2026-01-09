# Changelog for `fixed` Branch

## v1.0.5-fixed
- Fix database connection leaks in `OrderService.java`:
  - `getOrderItemsWithValidation()` always closes connection using try-with-resources
  - `getShippingInfoDetailed()` always closes connection using try-with-resources
  - Improved error handling and comments for maintainability
