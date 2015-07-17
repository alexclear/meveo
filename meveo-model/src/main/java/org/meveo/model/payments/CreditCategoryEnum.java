/*
* (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.meveo.model.payments;

/**
 * Credit Category Enum for Customer Account.
 */
public enum CreditCategoryEnum {

    PART_C(1, "creditCategory.part_c"),
    PART_M(2, "creditCategory.part_m"),
    PRO(3, "creditCategory.pro"),
    VIP(4, "creditCategory.vip"),
    NEWCUSTOMER(5, "creditCategory.newCustomer");

    private Integer id;
    private String label;

    CreditCategoryEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public Integer getId() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public static CreditCategoryEnum getValue(Integer id) {
        if (id != null) {
            for (CreditCategoryEnum status : values()) {
                if (id.equals(status.getId())) {
                    return status;
                }
            }
        }
        return null;
    }
}