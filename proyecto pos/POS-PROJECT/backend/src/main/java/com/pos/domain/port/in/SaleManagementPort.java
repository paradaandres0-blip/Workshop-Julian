package com.pos.domain.port.in;

import com.pos.domain.model.Sale;

public interface SaleManagementPort {
    Sale createSale();
    Sale addItem(String saleId, AddItemCommand cmd);
    Sale confirmSale(String saleId);
    Sale getSale(String saleId);

    record AddItemCommand(String productId, int quantity) {}
}
