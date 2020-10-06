package life.wanderinglocal;

import androidx.annotation.NonNull;

/**
 * WLCategory is currently used as a search filter model. As this app gets built out I
 * anticipate that model might change.
 * Todo list includes category provider (yelp, google, wanderinglocal, etc), search options (sortBy, etc)
 */
public class WLCategory {
    public WLCategory(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    private String name = "";

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Name: %s", name);
    }
}
