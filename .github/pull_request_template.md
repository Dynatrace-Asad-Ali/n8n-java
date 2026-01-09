Fix database connection leaks and improve resource handling

## Root Cause
The methods `getOrderItemsWithValidation()` and `getShippingInfoDetailed()` failed to close database connections on conditional branches, leading to connection leaks under specific triggers (`orderId % 500 == 0` and `orderId % 20 == 0`, respectively). This PR refactors these paths to use try-with-resources, ensuring connections, statements, and result sets are always closed. Comments and error handling were improved for maintainability.
