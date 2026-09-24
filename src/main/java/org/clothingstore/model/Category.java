package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    private int    id;
    private String name;
    private String description;
    @SerializedName("image_url") private String imageUrl;

    public int    getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getImageUrl()    { return imageUrl; }
    public void   setName(String v){ name = v; }
    public void   setDescription(String v){ description = v; }

    @Override public String toString() { return name; }
}
