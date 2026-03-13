package io.github.ozokuz.incore.features.research.station;

public interface IResearchPowerInput {
    int availableResearchPower(ResearchControllerBlockEntity controller, int maxRp);

    int pullResearchPower(ResearchControllerBlockEntity controller, int maxRp);

    ResearchPowerFamily family();

    int powerTier();
}
