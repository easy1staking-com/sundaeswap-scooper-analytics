package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressType;
import com.bloxbean.cardano.client.address.util.AddressEncoderDecoderUtil;
import com.bloxbean.cardano.client.address.util.AddressUtil;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.Optional.empty;

@Service
@Slf4j
public class AddressService {

    public Optional<Address> extractShelleyAddress(String address) {
        try {
            var addressBytes = AddressUtil.addressToBytes(address);
            var addressType = AddressEncoderDecoderUtil.readAddressType(addressBytes);
            if (AddressType.Byron.equals(addressType)) {
                return empty();
            } else {
                return Optional.of(new Address(address));
            }
        } catch (AddressExcepion e) {
            return empty();
        }
    }

}
