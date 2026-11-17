package com.dantariun.sat_demo_map.network.model

data class AddressModel (
    val place_id: Long,
    val lat: String,
    val lon: String,
    val name: String,
    val display_name: String,
    )

/**
 * {
 *     "place_id": 206011290,
 *     "licence": "Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
 *     "osm_type": "node",
 *     "osm_id": 357834797,
 *     "lat": "37.4975406",
 *     "lon": "127.0268829",
 *     "class": "highway",
 *     "type": "bus_stop",
 *     "place_rank": 30,
 *     "importance": 0.000072980086976379,
 *     "addresstype": "highway",
 *     "name": "강남역",
 *     "display_name": "강남역, 서초대로78길, 서초2동, 서초구, 서울특별시, 06620, 대한민국",
 *     "boundingbox": [
 *       "37.4974906",
 *       "37.4975906",
 *       "127.0268329",
 *       "127.0269329"
 *     ]
 *   },
 */