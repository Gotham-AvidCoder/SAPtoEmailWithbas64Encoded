<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
<xsl:output method="text" omit-xml-declaration="yes" indent="no"/>
<xsl:template match="Upload[@file_type='INVOICE']">
Invoice_Number,Invoice_Date,Due_Date,Customer_Code,Customer_Reference,Line_Quantity,Line-Description,Line_Rate,Line_Charge_Type,Line_Vat_Amount,Line_Vat_Rate
<xsl:for-each select="row">
<xsl:value-of select="concat(Invoice_Number,',',Invoice_Date,',',Due_Date,',',Customer_Code,',',Customer_Reference,',',Line_Quantity,',',Line-Description,',',Line_Rate,',',Line_Charge_Type,',',Line_Vat_Amount,',',Line_Vat_Rate,'&#xA;')"/>
</xsl:for-each>
</xsl:template>
<xsl:template match="Upload[@file_type='PURCHASE_ORDER']">
Order_Number,Order_Date,Status,Subtotal_Incl_Tax,Requested_Service,Shipping_Cost,Custom_1,Custom_2,Custom_3,Item_Name,Item_SKU,Item_Unit_Price,Item_Quantity,Item_Unit_Weight,Warehouse_Location,Full_Name,First_Name,Last_Name,Address_Line_1,Address_Line_2,City,State,Postal_Code,Country,Company,Email,Phone,Notes
<xsl:for-each select="row">
<xsl:value-of select="concat(Order_Number,',',Order_Date,',',Status,',',Subtotal_Incl_Tax,',',Requested_Service,',',Shipping_Cost,',',Custom_1,',',Custom_2,',',Custom_3,',',Item_Name,',',Item_SKU,',',Item_Unit_Price,',',Item_Quantity,',',Item_Unit_Weight,',',Warehouse_Location,',',Full_Name,',',First_Name,',',Last_Name,',',Address_Line_1,',',Address_Line_2,',',City,',',State,',',Postal_Code,',',Country,',',Company,',',Email,',',Phone,',',Notes,'&#xA;')"/>
</xsl:for-each>
</xsl:template>
<xsl:template match="Upload[@file_type='SALES_ORDER']">
Sr_No,Name,Company_Name,Address,Phone_Number,Email
<xsl:for-each select="row">
<xsl:value-of select="concat(Sr_No,',',Name,',',Company_Name,',',Address,',',Phone_Number,',',Email,'&#xA;')"/>
</xsl:for-each>
</xsl:template>
</xsl:stylesheet>