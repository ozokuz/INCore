package io.github.ozokuz.incore.features.research.model;

import java.util.List;

public record ResearchCostDefinition(
        List<LogicModuleRequirement> requiredLogicModules,
        List<ResearchMaterialRequirement> requiredResearchMaterials,
        List<CostModifier> modifiers
) {
    public record LogicModuleRequirement(String moduleTier, int durabilityCost) {
    }

    public record ResearchMaterialRequirement(String materialId, int count) {
    }

    public record CostModifier(String id, double value) {
    }
}
