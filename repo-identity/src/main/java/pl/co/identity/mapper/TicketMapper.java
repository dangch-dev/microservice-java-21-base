package pl.co.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.entity.Ticket;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "assignedTo", source = "assignedTo")
    TicketResponse toResponse(Ticket ticket);
}
