package pl.co.identity.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import pl.co.identity.dto.TicketResponse;
import pl.co.identity.entity.Ticket;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-15T22:14:16+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class TicketMapperImpl implements TicketMapper {

    @Override
    public TicketResponse toResponse(Ticket ticket) {
        if ( ticket == null ) {
            return null;
        }

        TicketResponse.TicketResponseBuilder ticketResponse = TicketResponse.builder();

        ticketResponse.createdBy( ticket.getCreatedBy() );
        ticketResponse.assignedTo( ticket.getAssignedTo() );
        ticketResponse.id( ticket.getId() );
        ticketResponse.title( ticket.getTitle() );
        ticketResponse.description( ticket.getDescription() );
        ticketResponse.status( ticket.getStatus() );
        ticketResponse.createdAt( ticket.getCreatedAt() );
        ticketResponse.updatedAt( ticket.getUpdatedAt() );

        return ticketResponse.build();
    }
}
