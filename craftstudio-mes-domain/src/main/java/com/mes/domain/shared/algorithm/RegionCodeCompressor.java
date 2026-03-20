package com.mes.domain.shared.algorithm;

import java.util.Collection;
import java.util.Set;

public interface RegionCodeCompressor {
    void fillRegionCodes(Collection<String> regionCodes);
    void removeRegionCodes(Collection<String> regionCodes);
    Set<String> buildCompressedRegionCodes();
    void clean();
}
