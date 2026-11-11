
package com.medtrack.model;

public class Medicine {

    private int medicineId;
    private String medicineName;
    private String genericName;
    private String category;
    private String manufacturer;
    private String unit;
    private int reorderLevel;

    public Medicine() {
    }

    public Medicine(int medicineId, String medicineName,
                    String genericName, String category,
                    String manufacturer, String unit,
                    int reorderLevel) {

        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.genericName = genericName;
        this.category = category;
        this.manufacturer = manufacturer;
        this.unit = unit;
        this.reorderLevel = reorderLevel;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}
