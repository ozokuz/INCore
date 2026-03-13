package io.github.ozokuz.incore.features.researchv2.material;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface ResearchKubeJsEvents {
    EventGroup GROUP = EventGroup.of("INCoreEvents");
    EventHandler RESEARCH_MATERIALS = GROUP.startup("researchMaterials", () -> ResearchMaterialKubeEvent.class);
}
