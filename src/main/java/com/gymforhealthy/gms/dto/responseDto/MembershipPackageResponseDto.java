package com.gymforhealthy.gms.dto.responseDto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPackageResponseDto {

    private Long id;
    private String name;
    private Integer packageTotalHour;
    private Boolean isUnlimited;

}
