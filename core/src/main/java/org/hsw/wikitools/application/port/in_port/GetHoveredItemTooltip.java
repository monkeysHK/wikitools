package org.hsw.wikitools.application.port.in_port;

import org.hsw.wikitools.domain.value.WikiTooltip;

import java.util.Optional;

public interface GetHoveredItemTooltip {
    Optional<WikiTooltip> getHoveredItemTooltip(boolean toTemplateFormatting);
}
