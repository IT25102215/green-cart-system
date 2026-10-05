package com.greencart.feature.supplier;

import com.greencart.designpattern.member5supplier.factory.PurchaseOrderFactory;
import com.greencart.entity.PurchaseOrder;
import com.greencart.entity.PurchaseOrderStatus;
import com.greencart.entity.Supplier;
import com.greencart.service.*;
import com.greencart.util.InputValidation;
import com.greencart.util.PhoneValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class SupplierManagementController {

    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;
    private final ProductService productService;
    private final SupplierProductService supplierProductService;
    private final AuditService auditService;
    private final PurchaseOrderFactory purchaseOrderFactory;

    @GetMapping("/admin/suppliers")
    public String suppliers(Model model) {
        populateSupplierModel(model, new Supplier());
        return "admin/suppliers";
    }

    @GetMapping("/admin/suppliers/edit/{id}")
    public String supplierEdit(@PathVariable Long id, Model model) {
        populateSupplierModel(model, supplierService.get(id));
        return "admin/suppliers";
    }

    private void populateSupplierModel(Model model, Supplier supplier) {
        var suppliers = supplierService.all();
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplier", supplier);
        model.addAttribute("performance", suppliers.stream().map(purchaseOrderService::performanceFor).toList());
    }

    @PostMapping("/admin/suppliers/save")
    public String supplierSave(@ModelAttribute Supplier supplier,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String name = InputValidation.requireText(supplier.getName(), "Supplier name", 2, 150);
            String email = InputValidation.requireEmail(supplier.getEmail());
            String phone = PhoneValidation.requireTenDigits(supplier.getPhone());

            supplierService.findByName(name).ifPresent(existing -> {
                if (supplier.getId() == null || !existing.getId().equals(supplier.getId())) {
                    throw new IllegalArgumentException("A supplier with this name already exists");
                }
            });
            supplierService.findByEmail(email).ifPresent(existing -> {
                if (supplier.getId() == null || !existing.getId().equals(supplier.getId())) {
                    throw new IllegalArgumentException("A supplier with this email already exists");
                }
            });

            boolean isNew = supplier.getId() == null;
            supplier.setName(name);
            supplier.setEmail(email);
            supplier.setPhone(phone);
            if (supplier.getCompany() != null && !supplier.getCompany().isBlank()) {
                supplier.setCompany(InputValidation.requireText(supplier.getCompany(), "Company", 2, 150));
            }
            if (supplier.getAddress() != null && !supplier.getAddress().isBlank()) {
                supplier.setAddress(InputValidation.requireText(supplier.getAddress(), "Address", 3, 255));
            }

            Supplier saved = supplierService.save(supplier);
            auditService.log("SUPPLIER", isNew ? "CREATE" : "UPDATE", "Supplier", saved.getId(),
                    (isNew ? "Created" : "Updated") + " supplier '" + saved.getName() + "'", authentication);
            redirectAttributes.addFlashAttribute("success",
                    isNew ? "Supplier added successfully." : "Supplier updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not save supplier: " + safeMessage(ex));
        }
        return "redirect:/admin/suppliers";
    }

    @PostMapping("/admin/suppliers/delete/{id}")
    public String supplierDelete(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Supplier supplier = supplierService.get(id);
            String name = supplier.getName();
            supplierService.delete(id);
            auditService.log("SUPPLIER", "ARCHIVE", "Supplier", id,
                    "Archived supplier '" + name + "'", authentication);
            redirectAttributes.addFlashAttribute("success", "Supplier archived successfully. Historical purchase orders are preserved.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not archive supplier: " + safeMessage(ex));
        }
        return "redirect:/admin/suppliers";
    }

    @GetMapping("/admin/purchase-orders")
    public String purchaseOrders(Model model) {
        model.addAttribute("purchaseOrders", purchaseOrderService.all());
        model.addAttribute("suppliers", supplierService.all().stream().filter(Supplier::isActive).toList());
        model.addAttribute("products", productService.all());
        model.addAttribute("purchaseOrder", new PurchaseOrder());
        return "admin/purchase-orders";
    }

    @PostMapping("/admin/purchase-orders/save")
    public String purchaseOrderSave(@RequestParam Long supplierId,
                                    @RequestParam Long productId,
                                    @RequestParam Integer quantity,
                                    @RequestParam BigDecimal unitPrice,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate expectedDeliveryDate,
                                    @RequestParam(defaultValue = "DRAFT") PurchaseOrderStatus status,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (quantity == null || quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1");
            if (unitPrice == null || unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price must be zero or greater");
            if (status == PurchaseOrderStatus.RECEIVED) {
                throw new IllegalArgumentException("Create the purchase order first, then record the supplier delivery as RECEIVED");
            }

            var supplier = supplierService.get(supplierId);
            var product = productService.get(productId);

            // A purchase order automatically records/updates the supplier-product relationship,
            // so the Supplier page does not need a separate manual assignment form.
            supplierProductService.assign(supplier, product, unitPrice);

            // Member 5 Factory Pattern: the controller no longer constructs PurchaseOrder directly.
            PurchaseOrder purchaseOrder = purchaseOrderFactory.create(
                    supplier,
                    product,
                    quantity,
                    unitPrice,
                    expectedDeliveryDate,
                    status
            );

            PurchaseOrder saved = purchaseOrderService.save(purchaseOrder);
            auditService.log("SUPPLIER", "CREATE_PO", "PurchaseOrder", saved.getId(),
                    "Created purchase order for " + saved.getQuantity() + " x '" + saved.getProduct().getName()
                            + "' from '" + saved.getSupplier().getName() + "'", authentication);
            redirectAttributes.addFlashAttribute("success", "Purchase order created successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not create purchase order: " + safeMessage(ex));
        }
        return "redirect:/admin/purchase-orders";
    }

    @PostMapping("/admin/purchase-orders/{id}/status")
    public String purchaseOrderStatus(@PathVariable Long id,
                                      @RequestParam PurchaseOrderStatus status,
                                      @RequestParam(required = false) Integer receivedQuantity,
                                      @RequestParam(required = false) String deliveryNote,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder before = purchaseOrderService.get(id);
            PurchaseOrderStatus oldStatus = before.getStatus();
            int oldStock = before.getProduct().getStock() == null ? 0 : before.getProduct().getStock();
            PurchaseOrder updated = purchaseOrderService.updateDeliveryStatus(id, status, receivedQuantity, deliveryNote);
            int newStock = updated.getProduct().getStock() == null ? 0 : updated.getProduct().getStock();

            auditService.logChange("SUPPLIER", "PO_STATUS", "PurchaseOrder", id,
                    oldStatus.name(), updated.getStatus().name(),
                    "Purchase order status updated", authentication);
            if (newStock != oldStock) {
                auditService.logChange("INVENTORY", "SUPPLIER_RECEIPT", "Product", updated.getProduct().getId(),
                        String.valueOf(oldStock), String.valueOf(newStock),
                        "Supplier delivery changed stock for '" + updated.getProduct().getName() + "'", authentication);
            }

            redirectAttributes.addFlashAttribute("success",
                    status == PurchaseOrderStatus.RECEIVED
                            ? "Supplier delivery recorded and product stock updated."
                            : "Purchase order status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update purchase order: " + safeMessage(ex));
        }
        return "redirect:/admin/purchase-orders";
    }

    @PostMapping("/admin/purchase-orders/{id}/delete")
    public String purchaseOrderDelete(@PathVariable Long id,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrder purchaseOrder = purchaseOrderService.get(id);
            if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED) {
                throw new IllegalArgumentException("Received purchase orders are retained as delivery history");
            }
            purchaseOrderService.delete(id);
            auditService.log("SUPPLIER", "DELETE_PO", "PurchaseOrder", id,
                    "Deleted purchase order for '" + purchaseOrder.getProduct().getName() + "'", authentication);
            redirectAttributes.addFlashAttribute("success", "Purchase order deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not delete purchase order: " + safeMessage(ex));
        }
        return "redirect:/admin/purchase-orders";
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
