package org.hsw.wikitools.feature.copy_skull_id;

import org.hsw.wikitools.feature.copy_skull_id.app.FindFacingBlockSkull;
import org.hsw.wikitools.feature.copy_skull_id.app.Skull;

import java.util.Optional;

public class FindFacingBlockSkullStub implements FindFacingBlockSkull {
    Optional<Skull> skull;

    public FindFacingBlockSkullStub(Optional<Skull> skull) {
        this.skull = skull;
    }

    @Override
    public Optional<Skull> findFacingSkull() {
        return skull;
    }
}
