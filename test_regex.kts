val item = """{"type":"video","title":"Test","videoId":"12345","author":"Author","lengthSeconds":120,"videoThumbnails":[{"quality":"mqdefault","url":"/vi/12345/mqdefault.jpg","width":320,"height":180}]}"""
val thumbMatch = Regex("\"url\":\"([^\"]+)\"").findAll(item)
val firstThumb = thumbMatch.firstOrNull()?.groupValues?.get(1)
println(firstThumb)
