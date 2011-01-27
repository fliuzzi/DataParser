package com.where.atlas;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.where.data.parsers.citysearch.CSListing;
import com.where.data.parsers.citysearch.Category;
import com.where.data.parsers.citysearch.Offer;
import com.where.data.parsers.citysearch.Placelist;
import com.where.data.parsers.citysearch.RatingStat;
import com.where.data.parsers.citysearch.TermFreq;
import com.where.data.parsers.citysearch.Tip;
import com.where.data.parsers.citysearch.MicroReview;
import com.where.data.parsers.citysearch.Review;

import org.json.JSONArray;
import org.json.JSONObject;

import com.where.commons.feed.citysearch.search.query.twitter.Tweet;



public class CSPlace extends Place implements Serializable {

    private static final long serialVersionUID = -9129331290975892940L;

    private static final DecimalFormat DECIMAL =  new DecimalFormat("#,##0.0");
    
    private static final MathContext mc = new MathContext(1, RoundingMode.HALF_UP);
    
    private static final List<String> NO_VALUES = new ArrayList<String>();
    private static final List<Tip> NO_TIPS = new ArrayList<Tip>();
    private static final List<Review> NO_REVIEWS = new ArrayList<Review>();
    
    

    private String referenceId;
    private String subtitle;
    
    
    
    
    private String category;
    private List<Category> categories = new ArrayList<Category>();
    
    private List<TermFreq> termFreqs = new ArrayList<TermFreq>();
    
    private String neighborhood;
    private List<String> neighborhoods;
    private List<String> markets;
    
    
    private List<String> bullets;
    private List<String> attributes;
    private List<Tip> tips;
    private List<String> images;
    private List<Review> editorials;
    private List<Review> userReviews;
    private List<Placelist> lists = new ArrayList<Placelist>();
    
    
    
    private List<CSListing> because = new ArrayList<CSListing>();
    private List<CSListing> similar = new ArrayList<CSListing>();
    
    private String rating;
    private String reviewCount;
    
    private double myrating;
    
    private String webUrl;
    private String menuUrl;
    private String sendToUrl;
    private String emailUrl;
    private String staticMapUrl;
    private String thumbUrl;
    
    private String customerMessage;
    
    private String tagline;
    private String businessHours;
    private String parking;
    
    private String priceLevel;
    
    private String reservationId;
    private String reservationUrl;
    
    private double ppe;
    private double maxCap;
    
    private List<MicroReview> reviews;
    
    private Offer offer;
    
    private double distance;
    
    private long clicksRemained;
    
    private boolean notAdvertiser;
    
    private List<Tweet> recentTweets = new ArrayList<Tweet>();
    
    private boolean updated;
            
    ///////////////
    //Constructor// 
    public CSPlace() {
        setSource(Source.CS);
    }
    
    public List<Category> getCategories()
    {
        return categories;
    }
    
    public String getListingId() {
        return getNativeId();
    }

    public void setListingId(String listingId) {
        setNativeId(listingId);
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public List<Category> categories() {
        return categories;
    }
    
    public void addCategory(Category category) {
        categories.add(category);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPpe() {
        return ppe;
    }

    public void setPpe(double ppe) {
        this.ppe = ppe;
    }

    public double getMaxCap() {
        return maxCap;
    }

    public void setMaxCap(double maxCap) {
        this.maxCap = maxCap;
    }

    public List<Placelist> lists() {
        return lists;
    }

    public void setLists(List<Placelist> lists) {
        this.lists = lists;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }


    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }
    
    public void addNeighborhood(String n) {
        if(n == null) return;
        
        if(neighborhoods == null) neighborhoods = new ArrayList<String>();
        
        neighborhoods.add(n);
    }
    
    public List<String> neighborhoods() {
        if(neighborhoods == null) return NO_VALUES;
        
        return neighborhoods;
    }
    
    public void addMarket(String m) {
        if(m == null) return;
        
        if(markets == null) markets = new ArrayList<String>();
        
        markets.add(m);
    }
    
    public List<String> markets() {
        if(markets == null) return NO_VALUES;
        
        return markets;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public double getRating() {
        try {
            return rating == null ? 0 : Double.parseDouble(rating)/2;
        }
        catch(Exception ex) {
            return 0;
        }
    }
    
    public void setRating(String rating) {
        this.rating = rating;
    }
    
    public void setMyRating(double myrating) {
        this.myrating = myrating;
    }
    
    public double getMyRating() {
        return myrating/2;
    }
    
    public String getReviewCount() {
        if(reviewCount == null) return "0";
        return reviewCount;
    }
    
    public void setReviewCount(String reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public List<RatingStat> getRatingStats() {
        List<RatingStat> stats = new ArrayList<RatingStat>();
        
        RatingStat five = new RatingStat(5);
        RatingStat four = new RatingStat(4);
        RatingStat three = new RatingStat(3);
        RatingStat two = new RatingStat(2);
        RatingStat one = new RatingStat(1);
        
        int countall = 0;
        if(!userReviews().isEmpty()) {
            for(Review review:userReviews) {
                if((int)review.getRating() == 5) five.setCount(five.getCount()+1);
                if((int)review.getRating() == 4) four.setCount(four.getCount()+1);
                if((int)review.getRating() == 3) three.setCount(three.getCount()+1);
                if((int)review.getRating() == 2) two.setCount(two.getCount()+1);
                if((int)review.getRating() == 1) one.setCount(one.getCount()+1);
            }
        
            countall = userReviews.size();
        }
        
        if(countall > 0) {
            five.setPercentage((int)(five.getCount()*100/countall));
            four.setPercentage((int)(four.getCount()*100/countall));
            three.setPercentage((int)(three.getCount()*100/countall));
            two.setPercentage((int)(two.getCount()*100/countall));
            one.setPercentage((int)(one.getCount()*100/countall));
        }
        
        stats.add(five);
        stats.add(four);
        stats.add(three);
        stats.add(two);
        stats.add(one);
        
        return stats;
    }
    
    public String getMenuUrl() {
        return menuUrl;
    }
    
    public void setMenuUrl(String menuUrl) {
        if(menuUrl != null) {
            if(menuUrl.indexOf("?") == -1) {
                menuUrl = menuUrl + "?t=" + System.currentTimeMillis();
            }
            else menuUrl = menuUrl + "&t=" + System.currentTimeMillis();
        }       
        
        this.menuUrl = menuUrl;
    }
    
    public String getWebUrl() {
        return webUrl;
    }
    
    public void setWebUrl(String webUrl) {
        if(webUrl != null) {
            if(webUrl.indexOf("?") == -1) {
                webUrl = webUrl + "?t=" + System.currentTimeMillis();
            }
            else webUrl = webUrl + "&t=" + System.currentTimeMillis();
        }       
        
        this.webUrl = webUrl;
    }
    
    public String getSendToUrl() {
        return sendToUrl;
    }
    
    public void setSendToUrl(String sendToUrl) {
        if(sendToUrl != null) {
            if(sendToUrl.indexOf("?") == -1) {
                sendToUrl = sendToUrl + "?t=" + System.currentTimeMillis();
            }
            else sendToUrl = sendToUrl + "&t=" + System.currentTimeMillis();
        }       
        
        this.sendToUrl = sendToUrl;
    }
    
    public String getEmailUrl() {
        return emailUrl;
    }
    
    public void setEmailUrl(String emailUrl) {
        if(emailUrl != null) {
            if(emailUrl.indexOf("?") == -1) {
                emailUrl = emailUrl + "?t=" + System.currentTimeMillis();
            }
            else emailUrl = emailUrl + "&t=" + System.currentTimeMillis();
        }       
        
        this.emailUrl = emailUrl;
    }
    
    public String getStaticMapUrl() {
        return staticMapUrl;
    }
    
    public void setStaticMapUrl(String staticMapUrl) {
        if(staticMapUrl != null) {
            if(staticMapUrl.indexOf("?") == -1) {
                staticMapUrl = staticMapUrl + "?t=" + System.currentTimeMillis();
            }
            else staticMapUrl = staticMapUrl + "&t=" + System.currentTimeMillis();
        }       
        
        this.staticMapUrl = staticMapUrl;
    }
    
    public String getCustomerMessage() {
        return customerMessage;
    }
    
    public void setCustomerMessage(String customerMessage) {
        this.customerMessage = customerMessage;
    }
    
    public String getTagline() {
        return tagline;
    }
    
    public void setTagline(String tagline) {
        this.tagline = tagline;
    }
    
    public String getBusinessHours() {
        return businessHours;
    }
    
    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }
    
    public String getParking() {
        return parking;
    }
    
    public void setParking(String parking) {
        this.parking = parking;
    }
    
    public String getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
    
    public String getReservationUrl() {
        return reservationUrl;
    }

    public void setReservationUrl(String reservationUrl) {
        this.reservationUrl = reservationUrl;
    }

    public void addBullet(String bullet) {
        if(bullet == null) return;
        
        if(bullets == null) bullets = new ArrayList<String>();
        
        bullets.add(bullet);
    }
    
    public List<String> bullets() {
        if(bullets == null) return NO_VALUES;
        
        return bullets;
    }
    
    public void addAttribute(String attribute) {
        if(attribute == null) return;
        
        if(attributes == null) attributes = new ArrayList<String>();
        
        attributes.add(attribute);
    }   
    
    public List<String> attributes() {
        if(attributes == null) return NO_VALUES;
        
        return attributes;
    }   
    
    public void addTip(Tip tip) {
        if(tip == null) return;
        
        if(tips == null) tips = new ArrayList<Tip>();
        
        tips.add(tip);
    }   
    
    public List<Tip> tips() {
        if(tips == null) return NO_TIPS;
        
        return tips;
    }   
    
    public List<TermFreq> getTermFreqs() {
        return termFreqs;
    }

    public void setTermFreqs(List<TermFreq> termFreqs) {
        this.termFreqs = termFreqs;
    }

    public void addImage(String image) {
        if(image == null) return;
        
        if(images == null) images = new ArrayList<String>();
        
        images.add(image);
    }   
    
    public List<String> images() {
        if(images == null) return NO_VALUES;
        
        return images;
    }   
    
    public void addEditorial(Review editorial) {
        if(editorial == null) return;
        
        if(editorials == null) editorials = new ArrayList<Review>();
        
        editorials.add(editorial);
    }   
    
    public List<Review> editorials() {
        if(editorials == null) return NO_REVIEWS;
        
        return editorials;
    }   
    
    public void addUserReview(Review userReview) {
        if(userReview == null) return;
        
        if(userReviews == null) userReviews = new ArrayList<Review>();
        
        userReviews.add(userReview);
    }
    
    public List<Review> userReviews() {
        if(userReviews == null) return NO_REVIEWS;
        
        return userReviews;
    }
    public void setMicroReviews(List<MicroReview> reviews) {
        this.reviews = reviews;
    }
    public List<MicroReview> reviews() {
        return reviews;
    }   
    public Offer getOffer() {
        return offer;
    }
    public void setOffer(Offer offer) {
        this.offer = offer;
    }
    public String getPriceLevel() {
        return priceLevel;
    }
    public void setPriceLevel(String priceLevel) {
        this.priceLevel = priceLevel;
    }
    
    public void addRecentTweets(List<Tweet> tweets) {
        recentTweets.addAll(tweets);
    }
    
    public List<Tweet> recentTweets() {
        return recentTweets;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public long getClicksRemained() {
        return clicksRemained;
    }

    public void setClicksRemained(long clicksRemained) {
        this.clicksRemained = clicksRemained;
    }

    public boolean isNotAdvertiser() {
        return notAdvertiser;
    }

    public void setNotAdvertiser() {
        notAdvertiser = true;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
    
    public void setBecause(List<CSListing> because) {
        this.because = because;
    }
    public List<CSListing> getBecause() {
        return because;
    }
    public void setSimilar(List<CSListing> similar) {
        this.similar = similar;
    }   
    public List<CSListing> getSimilar() {
        return similar;
    }

    @Override
    public int hashCode() {
        return getListingId().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof CSListing)) return false;
        
        return getListingId().equals(((CSPlace)obj).getListingId());
    }
    
    public JSONObject toJSONSnippet() {
        try {
            JSONObject json = new JSONObject();
            if(this.getSource() != null)
            {
                json.put("source", this.getSource().toString());                     
            }
            JSONArray ids = new JSONArray();
            JSONObject id = new JSONObject();
            id.put("cs", getListingId());
            ids.put(id);
            
            if(getWhereId() != null) {
                id = new JSONObject();
                id.put("where", getWhereId());
                ids.put(id);
            }
            json.put("ids", ids);
            
            json.put("name", getName());
            
            if(thumbUrl != null) {
                json.put("thumburl", thumbUrl);
            }
            
            JSONObject location = getAddress().toJSON();
            if(neighborhood != null && neighborhood.trim().length() > 0) {
                location.put("neighborhood", neighborhood);
            }
            
            if(!categories.isEmpty()) {
                JSONArray a = new JSONArray();
                for(Category c:categories) {
                    a.put(c.toJSON());
                }
                json.put("categories", a);
            }
            
            json.put("location", location);
            
            if(getPhone() != null) {
                json.put("phone", getPhone());
            }
            
            json.put("rating", getRating());
            
            List<RatingStat> stats = getRatingStats();
            if(!stats.isEmpty()) {
                JSONArray a = new JSONArray();
                for(RatingStat r:stats) {
                    a.put(r.toJSON());
                }
                json.put("ratingStats", a);
            }
            
            if(distance > 0) {
                BigDecimal dist = new BigDecimal(distance);
                dist = dist.round(mc);
                json.put("distance", DECIMAL.format(dist));
            }
            
            if(because != null && !because.isEmpty()) {
                CSListing b = because.get(0);
                json.put("because", b.toJSONSnippet());
            }
            
            return json;
        }
        catch(Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public JSONObject toJSON() {
        return toJSON(true);
    }
    
    public JSONObject toJSON(boolean includeReviews) {
        try {
            JSONObject json = toJSONSnippet();
            
            if(!bullets().isEmpty()) {
                JSONArray a = new JSONArray();
                for(String b:bullets) {
                    a.put(b);
                }
                json.put("bullets", a);
            }
            
            if(!attributes().isEmpty()) {
                JSONArray a = new JSONArray();
                for(String at:attributes) {
                    a.put(at);
                }
                json.put("attributes", a);
            }
            
            if(!tips().isEmpty()) {
                JSONArray a = new JSONArray();
                for(Tip t:tips) {
                    a.put(t.toJSON());
                }
                json.put("tips", a);
            }
            
            if(!images().isEmpty()) {
                JSONArray a = new JSONArray();
                for(String i:images) {
                    a.put(i);
                }
                json.put("images", a);
            }
            
            if(includeReviews) {
                if(!editorials().isEmpty()) {
                    JSONArray a = new JSONArray();
                    for(Review e:editorials) {
                        a.put(e.toJSON());
                    }
                    json.put("editorials", a);
                }
                
                if(!userReviews().isEmpty()) {
                    JSONArray a = new JSONArray();
                    for(Review r:userReviews) {
                        a.put(r.toJSON());
                    }
                    json.put("reviews", a);
                }
            }
            else {
                if(!editorials().isEmpty()) {
                    json.put("review", editorials.get(0).toJSON());
                }
                else if(!userReviews().isEmpty()) {
                    json.put("review", userReviews.get(0).toJSON());
                }
            }
            
            json.put("reviewCount", reviewCount);
            
            List<RatingStat> stats = getRatingStats();
            if(!stats.isEmpty()) {
                JSONArray a = new JSONArray();
                for(RatingStat r:stats) {
                    a.put(r.toJSON());
                }
                json.put("ratingStats", a);
            }
            
            if(!getTermFreqs().isEmpty()) {
                JSONArray a = new JSONArray();
                for(TermFreq tf:termFreqs) {
                    a.put(tf.toJSON());
                }
                json.put("terms", a);
            }
            
            if(webUrl != null) {
                json.put("webUrl", webUrl);
            }
            
            if(menuUrl != null) {
                json.put("menuUrl", menuUrl);
            }
            
            if(sendToUrl != null) {
                json.put("sendToUrl", sendToUrl);
            }
            
            if(emailUrl != null) {
                json.put("emailUrl", emailUrl);
            }
            
            if(staticMapUrl != null) {
                json.put("staticMapUrl", staticMapUrl);
            }       
            
            if(customerMessage != null) {
                json.put("customerMessage", customerMessage);
            }       
            
            if(tagline != null) {
                json.put("tagline", tagline);
            }   
            
            if(businessHours != null) {
                json.put("businessHours", businessHours);
            }   
            
            if(parking != null) {
                json.put("parking", parking);
            }   
            
            if(priceLevel != null) {
                json.put("priceLevel", priceLevel);
            }   
    
            if(reservationUrl != null) {
                json.put("reservationUrl", reservationUrl);
            }   
            
            if(offer != null) {
                json.put("offer", offer.toJSON());
            }
            
            if(!lists.isEmpty()) {
                JSONArray a = new JSONArray();
                for(Placelist list:lists) {
                    a.put(list.toJSON());
                }
                json.put("lists", a);
            }
            
            return json;
        }
        catch(Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public JSONObject reviewsToJSON() {
        try {
            JSONObject json = toJSONSnippet();
            
            if(editorials() != null && !editorials().isEmpty()) {
                JSONArray a = new JSONArray();
                for(Review e:editorials) {
                    a.put(e.toJSON());
                }
                json.put("editorials", a);
            }
            
            if(userReviews() != null && !userReviews().isEmpty()) {
                JSONArray a = new JSONArray();
                for(Review r:userReviews) {
                    a.put(r.toJSON());
                }
                json.put("reviews", a);
            }
            
            json.put("reviewCount", reviewCount);
            
            return json;
        }
        catch(Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

}