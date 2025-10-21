package com.dantariun.sat_demo_map.domain.usecase

import com.dantariun.sat_demo_map.network.MainRepository
import com.dantariun.sat_demo_map.network.model.AddressModel
import retrofit2.Response
import javax.inject.Inject

class SearchAddressUseCase @Inject constructor(
    private val repository: MainRepository
) {
    suspend operator fun invoke(word: String): Response<List<AddressModel>> {
        return repository.searchAddress(word)
    }
}