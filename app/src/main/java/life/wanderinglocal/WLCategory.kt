package life.wanderinglocal

/**
 * WLCategory is currently used as a search filter model. As this app gets built out I
 * anticipate that model might change.
 * Todo list includes category provider (yelp, google, wanderinglocal, etc), search options (sortBy, etc)
 */
class WLCategory(name: String) {
    var name = ""

    override fun toString(): String {
        return String.format("Name: %s", name)
    }

    init {
        this.name = name
    }
}