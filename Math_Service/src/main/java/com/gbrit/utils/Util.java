package com.gbrit.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class Util {
    /**
     * Generates a collection name based on the class name and organization name with ID.
     *
     * @param obj                    The object for which the collection name is generated.
     * @param organizationNameWithId The organization-specific name with ID.
     * @return                       The generated collection name.
     */
    public static String getCollectionNameWithOrgName(Object obj, String organizationNameWithId) {
        // Retrieve the simple name of the class
        String className = obj.getClass().getSimpleName();
        // Combine class name and organization name with ID to form collection name
        return className + Constants.UNDERSCORE + organizationNameWithId;
    }

    public static String cleanUsername(String inputUsername) {
        if (inputUsername == null) {
            return null;
        }
        return inputUsername.replaceAll("\\s+", "");
    }

    public static Claims validateTokenAndGetClaims(String token) {
        try {
            String decodedToken = AesUtils.decryptAes(token, Constants.SECRET_AES_KEY, Constants.IV);
            // Token validation and user extraction
            return Jwts.parser().setSigningKey(Constants.SECRET_KEY).parseClaimsJws(decodedToken).getBody();
        } catch (JwtException e) {
            throw new JwtException("Invalid token");
        } catch (Exception e) {
            throw new RuntimeException("Error processing token");
        }
    }
}
