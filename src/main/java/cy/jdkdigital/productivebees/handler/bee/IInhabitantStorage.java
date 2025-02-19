package cy.jdkdigital.productivebees.handler.bee;

import cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntityAbstract;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nonnull;
import java.util.List;

public interface IInhabitantStorage
{
    @Nonnull
    List<AdvancedBeehiveBlockEntityAbstract.Inhabitant> getInhabitants();

    void setInhabitants(List<AdvancedBeehiveBlockEntityAbstract.Inhabitant> inhabitantList);

    void addInhabitant(AdvancedBeehiveBlockEntityAbstract.Inhabitant inhabitant);

    void clearInhabitants();

    @Nonnull
    ListTag getInhabitantListAsListNBT();

    void setInhabitantsFromListNBT(ListTag list);

    void onContentsChanged();
}
