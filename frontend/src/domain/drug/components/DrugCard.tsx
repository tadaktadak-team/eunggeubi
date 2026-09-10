import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { MaterialCommunityIcons, Feather } from '@expo/vector-icons';

interface DrugCardProps {
  name: string;        // 예: 타이레놀정
  dosage: string;      // 예: 500mg
  category: string;    // 예: 해열·진통제
  onPress?: () => void;
}

const DrugCard = ({ name, dosage, category, onPress }: DrugCardProps) => {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.7}>
      <View style={styles.iconContainer}>
        <MaterialCommunityIcons name="pill" size={24} color="#E53935" />
      </View>
      
      <View style={styles.infoContainer}>
        <Text style={styles.nameText}>{name}</Text>
        <Text style={styles.dosageText}>{dosage}</Text>
        <Text style={styles.categoryText}>{category}</Text>
      </View>
      
      <Feather name="chevron-right" size={20} color="#A0A0A0" />
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: '#FFF0F0',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  infoContainer: {
    flex: 1,
  },
  nameText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#111111',
  },
  dosageText: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#111111',
    marginTop: 2,
  },
  categoryText: {
    fontSize: 13,
    color: '#8E8E93',
    marginTop: 4,
  },
});

export default DrugCard;