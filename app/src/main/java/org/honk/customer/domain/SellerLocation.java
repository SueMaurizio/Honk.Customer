package org.honk.customer.domain;

import com.google.android.gms.maps.model.LatLng;

public class SellerLocation {
    public Seller seller;
    public LatLng location;

    public SellerLocation(String sellerName, String sellerDescription, Double latitude, Double longitude) {
        this.seller = new Seller(sellerName, sellerDescription);
        this.location = new LatLng(latitude, longitude);
    }
}
