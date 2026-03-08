package io.github.ozokuz.incore.features.researchv2.station;

public interface IResearchPowerInput {
    int availableResearchPower(ResearchControllerBlockEntity controller, int maxRp);

    int pullResearchPower(ResearchControllerBlockEntity controller, int maxRp);

    ResearchPowerFamily family();

    int powerTier();
}
