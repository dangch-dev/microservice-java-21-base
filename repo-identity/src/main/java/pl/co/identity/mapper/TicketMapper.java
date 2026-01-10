package pl.co.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.entity.Ticket;
import pl.co.identity.entity.TicketStatus;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "assignedTo", source = "assignedTo")
    @Mapping(target = "files", source = "files")
    TicketResponse toResponse(Ticket ticket);

    default TicketStatus toTicketStatus(String status) {
        return status == null ? null : TicketStatus.valueOf(status);
    }
}
