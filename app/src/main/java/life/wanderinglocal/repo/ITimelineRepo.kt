package life.wanderinglocal.repo

interface ITimelineRepo {
    fun setLocation(location : String)
    fun setLocation(lat : String, lng : String)
    fun getSearchingBy()

}